package cn.kaziki.nft.turbo.synthesis.domain.service.impl;

import cn.kaziki.nft.turbo.api.synthesis.request.SynthesisRecipeCreateRequest;
import cn.kaziki.nft.turbo.api.synthesis.request.SynthesisRecipeModifyRequest;
import cn.kaziki.nft.turbo.api.album.event.AlbumRefreshEvent;
import cn.kaziki.nft.turbo.api.chain.constant.ChainOperateBizTypeEnum;
import cn.kaziki.nft.turbo.api.chain.request.ChainProcessRequest;
import cn.kaziki.nft.turbo.api.chain.response.ChainProcessResponse;
import cn.kaziki.nft.turbo.api.chain.response.data.ChainOperationData;
import cn.kaziki.nft.turbo.api.chain.service.ChainFacadeService;
import cn.kaziki.nft.turbo.api.collection.constant.CollectionRarity;
import cn.kaziki.nft.turbo.api.collection.constant.GoodsSaleBizType;
import cn.kaziki.nft.turbo.api.collection.model.CollectionVO;
import cn.kaziki.nft.turbo.api.collection.model.HeldCollectionVO;
import cn.kaziki.nft.turbo.api.collection.request.HeldCollectionCreateDTO;
import cn.kaziki.nft.turbo.api.collection.service.CollectionManageFacadeService;
import cn.kaziki.nft.turbo.api.collection.service.CollectionReadFacadeService;
import cn.kaziki.nft.turbo.api.goods.constant.GoodsType;
import cn.kaziki.nft.turbo.api.star.constant.StarChangeType;
import cn.kaziki.nft.turbo.api.star.request.StarChangeRequest;
import cn.kaziki.nft.turbo.api.star.service.StarAccountFacadeService;
import cn.kaziki.nft.turbo.api.synthesis.model.SynthesisBroadcastVO;
import cn.kaziki.nft.turbo.api.synthesis.request.SynthesisSubmitRequest;
import cn.kaziki.nft.turbo.base.response.PageResponse;
import cn.kaziki.nft.turbo.base.response.SingleResponse;
import cn.kaziki.nft.turbo.base.utils.RemoteCallWrapper;
import cn.kaziki.nft.turbo.synthesis.domain.constant.SynthesisStateEnum;
import cn.kaziki.nft.turbo.synthesis.domain.entity.SynthesisRecipe;
import cn.kaziki.nft.turbo.synthesis.domain.entity.SynthesisStream;
import cn.kaziki.nft.turbo.synthesis.domain.service.SynthesisService;
import cn.kaziki.nft.turbo.synthesis.exception.SynthesisException;
import cn.kaziki.nft.turbo.synthesis.infrastructure.mapper.SynthesisRecipeMapper;
import cn.kaziki.nft.turbo.synthesis.infrastructure.mapper.SynthesisStreamMapper;
import cn.kaziki.turbo.stream.producer.StreamProducer;
import cn.hutool.core.lang.Assert;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static cn.kaziki.nft.turbo.synthesis.exception.SynthesisErrorCode.*;

/**
 * 合成领域服务
 */
@Slf4j
@Service
public class SynthesisServiceImpl extends ServiceImpl<SynthesisStreamMapper, SynthesisStream>
        implements SynthesisService {

    @Autowired
    private SynthesisRecipeMapper synthesisRecipeMapper;
    @Autowired
    private StreamProducer streamProducer;
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private CollectionReadFacadeService collectionReadFacadeService;
    @Autowired
    private CollectionManageFacadeService collectionManageFacadeService;
    @Autowired
    private StarAccountFacadeService starAccountFacadeService;
    @Autowired
    private ChainFacadeService chainFacadeService;

    // 提交合成：锁定材料 → 写流水（前置校验由 Validator 链完成）
    @Transactional(rollbackFor = Exception.class)
    @Override
    public SynthesisStream submit(SynthesisSubmitRequest request, String userId) {
        // 1. 幂等校验
        SynthesisStream existStream = baseMapper.selectByIdentifier(request.getIdentifier());
        if (existStream != null) {
            return existStream;
        }

        // 2. 查配方（写流水需要字段）
        SynthesisRecipe recipe = synthesisRecipeMapper.selectById(request.getRecipeId());

        // 3. 查材料卡铸造值
        List<Long> materialIds = request.getMaterialIds().stream()
                .map(Long::valueOf).collect(Collectors.toList());
        SingleResponse<List<HeldCollectionVO>> materialsResp =
                collectionReadFacadeService.batchQueryHeldCollections(materialIds);
        Assert.isTrue(materialsResp.getSuccess() && materialsResp.getData() != null,
                () -> new SynthesisException(MATERIAL_NOT_OWNED));
        long materialForge = materialsResp.getData().stream()
                .mapToLong(material -> material.getForgeValue() != null ? material.getForgeValue() : 0L).sum();

        // 4. 锁定材料卡
        SingleResponse<Boolean> lockResp =
                collectionManageFacadeService.batchUpdateHeldCollectionState(materialIds, "LOCKED", userId);
        Assert.isTrue(lockResp.getSuccess(),
                () -> new SynthesisException(MATERIAL_NOT_OWNED));

        // 5. 写合成流水
        SynthesisStream stream = new SynthesisStream();
        stream.setIdentifier(request.getIdentifier());
        stream.setUserId(userId);
        stream.setRecipeId(recipe.getId());
        stream.setSeriesId(recipe.getSeriesId());
        stream.setSourceRarity(recipe.getSourceRarity());
        stream.setTargetRarity(recipe.getTargetRarity());
        stream.setCardCount(recipe.getCardCount());
        stream.setStarCost(recipe.getStarCost());
        stream.setForgeValueBefore(materialForge);
        stream.setForgeValueAfter(CollectionRarity.valueOf(recipe.getTargetRarity()).getBaseForgeValue());
        stream.setState(SynthesisStateEnum.SUBMITTED.name());
        stream.setMaterialIds(materialIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        stream.setGmtCreate(new Date());
        stream.setGmtModified(new Date());
        var saveResult = save(stream);
        Assert.isTrue(saveResult, () -> new SynthesisException(STREAM_SAVE_FAILED));

        return stream;
    }

    // 异步执行合成链：状态机驱动，支持幂等重试
    @Override
    public void executeAsync(SynthesisStream stream) {
        try {
            // 重新加载最新状态（防并发/重试读到过期数据）
            SynthesisStream latest = getById(stream.getId());
            if (latest == null || SynthesisStateEnum.MINTED.name().equals(latest.getState()) || SynthesisStateEnum.FAILED.name().equals(latest.getState())) {
                log.info("合成已完成或失败，跳过执行，synthesisId={}", stream.getIdentifier());
                return;
            }
            stream = latest;

            // 阶段1: 锁定材料（幂等）
            if (SynthesisStateEnum.SUBMITTED.name().equals(stream.getState())) {
                stream.setState(SynthesisStateEnum.LOCKED.name());
                updateById(stream);
            }

            // 阶段2: 销毁材料卡（链操作幂等）
            if (SynthesisStateEnum.LOCKED.name().equals(stream.getState())) {
                stream.setState(SynthesisStateEnum.BURNING.name());
                updateById(stream);

                List<Long> materialIdList = parseMaterialIds(stream.getMaterialIds());
                SingleResponse<List<HeldCollectionVO>> materialsResp =
                        collectionReadFacadeService.batchQueryHeldCollections(materialIdList);
                Assert.isTrue(materialsResp.getSuccess() && materialsResp.getData() != null,
                        () -> new SynthesisException("查询材料卡失败", STREAM_SAVE_FAILED));

                for (HeldCollectionVO material : materialsResp.getData()) {
                    ChainProcessRequest burnRequest = new ChainProcessRequest();
                    burnRequest.setIdentifier(stream.getIdentifier() + "_BURN_" + material.getId());
                    burnRequest.setBizId(material.getId());
                    burnRequest.setBizType(ChainOperateBizTypeEnum.HELD_COLLECTION.name());
                    burnRequest.setClassId(material.getCollectionId() != null
                            ? material.getCollectionId().toString() : material.getId());
                    burnRequest.setSerialNo(material.getSerialNo());
                    ChainProcessResponse<ChainOperationData> burnResp =
                            RemoteCallWrapper.call(req -> chainFacadeService.destroy(req), burnRequest,
                                    "synthesisBurn");
                    Assert.isTrue(burnResp.getSuccess(),
                            () -> new SynthesisException("链上销毁失败", STREAM_SAVE_FAILED));
                }
            }

            // 阶段3: 扣星尘（幂等）
            if (SynthesisStateEnum.BURNING.name().equals(stream.getState())) {
                stream.setState(SynthesisStateEnum.BURNED.name());
                updateById(stream);

                StarChangeRequest starRequest = new StarChangeRequest();
                starRequest.setUserId(stream.getUserId());
                starRequest.setAmount(stream.getStarCost());
                starRequest.setChangeType(StarChangeType.SPEND);
                starRequest.setBizNo(stream.getIdentifier());
                starRequest.setIdentifier(stream.getIdentifier() + "_STAR");
                starAccountFacadeService.decrease(starRequest);
            }

            // 阶段4: 铸造产物（链操作幂等）
            if (SynthesisStateEnum.BURNED.name().equals(stream.getState())) {
                stream.setState(SynthesisStateEnum.MINTING.name());
                updateById(stream);

                // 查配方判断类型
                SynthesisRecipe recipe = synthesisRecipeMapper.selectById(stream.getRecipeId());
                Assert.notNull(recipe, () -> new SynthesisException(RECIPE_NOT_FOUND));

                // 获取目标藏品
                CollectionVO targetCollection;
                if ("CARD_BASED".equals(recipe.getRecipeType())) {
                    SingleResponse<CollectionVO> targetResp =
                            collectionReadFacadeService.queryById(recipe.getTargetCollectionId());
                    Assert.isTrue(targetResp.getSuccess() && targetResp.getData() != null,
                            () -> new SynthesisException(TARGET_SOLD_OUT));
                    targetCollection = targetResp.getData();
                    // 二次校验：合成期间是否被下架/售罄
                    Assert.isTrue("SUCCEED".equals(targetCollection.getCollectionState())
                                    && targetCollection.getInventory() != null
                                    && targetCollection.getInventory() > 0,
                            () -> new SynthesisException(TARGET_SOLD_OUT));
                } else {
                    SingleResponse<CollectionVO> targetResp =
                            collectionReadFacadeService.querySynthesisTarget(
                                    stream.getSeriesId(), stream.getTargetRarity());
                    Assert.isTrue(targetResp.getSuccess() && targetResp.getData() != null,
                            () -> new SynthesisException(TARGET_SOLD_OUT));
                    targetCollection = targetResp.getData();
                }

                // 创建产物持有记录
                HeldCollectionCreateDTO createDTO = new HeldCollectionCreateDTO();
                createDTO.setIdentifier(stream.getIdentifier() + "_PRODUCT");
                createDTO.setName(targetCollection.getName());
                createDTO.setCover(targetCollection.getCover());
                createDTO.setBizNo(stream.getIdentifier());
                createDTO.setBizType(GoodsSaleBizType.SYNTHESIS.name());
                createDTO.setUserId(stream.getUserId());
                createDTO.setGoodsId(targetCollection.getId());
                createDTO.setGoodsType(GoodsType.COLLECTION.name());
                createDTO.setRarity(CollectionRarity.valueOf(stream.getTargetRarity()));
                createDTO.setForgeValue(stream.getForgeValueAfter());
                createDTO.setSerialNoBaseId("SYNTH_" + stream.getIdentifier());

                SingleResponse<HeldCollectionVO> productResp =
                        collectionManageFacadeService.createHeldCollection(createDTO);
                Assert.isTrue(productResp.getSuccess() && productResp.getData() != null,
                        () -> new SynthesisException(STREAM_SAVE_FAILED));
                HeldCollectionVO product = productResp.getData();

                // 链上铸造
                ChainProcessRequest mintRequest = new ChainProcessRequest();
                mintRequest.setIdentifier(stream.getIdentifier() + "_MINT");
                mintRequest.setBizId(product.getId());
                mintRequest.setBizType(ChainOperateBizTypeEnum.HELD_COLLECTION.name());
                mintRequest.setClassId(targetCollection.getId().toString());
                mintRequest.setClassName(targetCollection.getName());
                mintRequest.setSerialNo(product.getSerialNo());
                ChainProcessResponse<ChainOperationData> mintResp =
                        RemoteCallWrapper.call(req -> chainFacadeService.mint(req), mintRequest,
                                "synthesisMint");
                Assert.isTrue(mintResp.getSuccess(),
                        () -> new SynthesisException("链上铸造失败", STREAM_SAVE_FAILED));

                stream.setProductHeldId(Long.valueOf(product.getId()));
            }

            // 阶段5: 完成
            stream.setState(SynthesisStateEnum.MINTED.name());
            stream.setGmtModified(new Date());
            updateById(stream);

            // SP/UR 全平台广播
            if (CollectionRarity.UNIQUE.name().equals(stream.getTargetRarity())
                    || CollectionRarity.MYTHICAL.name().equals(stream.getTargetRarity())) {
                broadcastSynthesis(stream);
            }

            // 刷新图鉴进度（异步）
            eventPublisher.publishEvent(
                    new AlbumRefreshEvent(this, stream.getUserId(), stream.getSeriesId()));

        } catch (SynthesisException e) {
            log.error("合成执行失败，synthesisId={}，state={}",
                    stream.getIdentifier(), stream.getState(), e);
            markFailed(stream);
            if (!SynthesisStateEnum.BURNED.name().equals(stream.getState()) && !SynthesisStateEnum.MINTING.name().equals(stream.getState())) {
                unlockMaterials(stream.getMaterialIds(), stream.getUserId());
            }
        } catch (Exception e) {
            log.error("合成执行异常，synthesisId={}", stream.getIdentifier(), e);
            markFailed(stream);
            unlockMaterials(stream.getMaterialIds(), stream.getUserId());
        }
    }

    // 标记合成失败
    private void markFailed(SynthesisStream stream) {
        stream.setState(SynthesisStateEnum.FAILED.name());
        stream.setGmtModified(new Date());
        updateById(stream);
    }

    // 解锁材料卡（执行失败时回退）
    private void unlockMaterials(String materialIds, String userId) {
        if (materialIds == null) {
            return;
        }
        try {
            List<Long> ids = parseMaterialIds(materialIds);
            collectionManageFacadeService.batchUpdateHeldCollectionState(ids, "ACTIVED", userId);
        } catch (Exception e) {
            log.error("解锁材料卡失败，materialIds={}", materialIds, e);
        }
    }

    private List<Long> parseMaterialIds(String materialIds) {
        return Arrays.stream(materialIds.split(","))
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }

    // 查询合成进度
    @Override
    public SynthesisStream queryByIdentifier(String identifier) {
        return baseMapper.selectByIdentifier(identifier);
    }

    // 分页查询配方（C 端/Admin）
    @Override
    public List<SynthesisRecipe> pageQueryRecipe(Long seriesId) {
        QueryWrapper<SynthesisRecipe> wrapper = new QueryWrapper<>();
        wrapper.eq("state", "ACTIVE");
        if (seriesId != null) {
            wrapper.eq("series_id", seriesId);
        }
        return synthesisRecipeMapper.selectList(wrapper);
    }

    // Artist 查询自己的配方
    @Override
    public List<SynthesisRecipe> pageQueryMyRecipes(String creatorId, Long seriesId) {
        QueryWrapper<SynthesisRecipe> wrapper = new QueryWrapper<>();
        wrapper.eq("creator_id", creatorId);
        if (seriesId != null) {
            wrapper.eq("series_id", seriesId);
        }
        wrapper.orderByDesc("gmt_create");
        return synthesisRecipeMapper.selectList(wrapper);
    }

    // 分页查询我的合成历史
    @Override
    public PageResponse<SynthesisStream> pageQueryMyStream(String userId, int currentPage, int pageSize) {
        List<SynthesisStream> list = baseMapper.selectByUserId(userId);
        return PageResponse.of(list, list.size(), pageSize, currentPage);
    }

    // 创建配方
    @Transactional(rollbackFor = Exception.class)
    @Override
    public SynthesisRecipe createRecipe(
            SynthesisRecipeCreateRequest request) {
        SynthesisRecipe recipe = new SynthesisRecipe();
        recipe.setSeriesId(request.getSeriesId());
        recipe.setName(request.getName());
        recipe.setSourceRarity(request.getSourceRarity());
        recipe.setTargetRarity(request.getTargetRarity());
        recipe.setCardCount(request.getCardCount());
        recipe.setStarCost(request.getStarCost());
        recipe.setState("ACTIVE");
        recipe.setCreatorId(request.getCreatorId());
        recipe.setRecipeType(request.getRecipeType() != null ? request.getRecipeType() : "RARITY_BASED");
        if (request.getSourceCollectionIds() != null && !request.getSourceCollectionIds().isEmpty()) {
            recipe.setSourceCollectionIds(
                    request.getSourceCollectionIds().stream()
                            .map(String::valueOf).collect(Collectors.joining(",")));
        }
        recipe.setTargetCollectionId(request.getTargetCollectionId());
        var result = synthesisRecipeMapper.insert(recipe);
        Assert.isTrue(result > 0, () -> new SynthesisException(STREAM_SAVE_FAILED));
        return recipe;
    }

    // 修改配方
    @Transactional(rollbackFor = Exception.class)
    @Override
    public SynthesisRecipe modifyRecipe(
            SynthesisRecipeModifyRequest request) {
        SynthesisRecipe recipe = synthesisRecipeMapper.selectById(request.getId());
        Assert.notNull(recipe, () -> new SynthesisException(RECIPE_NOT_FOUND));
        if (request.getName() != null) {
            recipe.setName(request.getName());
        }
        if (request.getSourceRarity() != null) {
            recipe.setSourceRarity(request.getSourceRarity());
        }
        if (request.getTargetRarity() != null) {
            recipe.setTargetRarity(request.getTargetRarity());
        }
        if (request.getCardCount() != null) {
            recipe.setCardCount(request.getCardCount());
        }
        if (request.getStarCost() != null) {
            recipe.setStarCost(request.getStarCost());
        }
        var result = synthesisRecipeMapper.updateById(recipe);
        Assert.isTrue(result > 0, () -> new SynthesisException(STREAM_SAVE_FAILED));
        return recipe;
    }

    // 启用/停用配方
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean toggleRecipe(Long id, String state) {
        SynthesisRecipe recipe = synthesisRecipeMapper.selectById(id);
        Assert.notNull(recipe, () -> new SynthesisException(RECIPE_NOT_FOUND));
        recipe.setState(state);
        var result = synthesisRecipeMapper.updateById(recipe);
        Assert.isTrue(result > 0, () -> new SynthesisException(STREAM_SAVE_FAILED));
        return true;
    }

    // 扫描超时锁定的合成流水（XXL-Job）
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void scanTimeoutLocks() {
        QueryWrapper<SynthesisStream> wrapper = new QueryWrapper<>();
        wrapper.eq("state", SynthesisStateEnum.LOCKED.name());
        wrapper.lt("gmt_modified", new Date(System.currentTimeMillis() - 30 * 60 * 1000));
        List<SynthesisStream> timeoutList = list(wrapper);
        for (SynthesisStream stream : timeoutList) {
            log.info("合成锁定超时，解锁材料，synthesisId={}", stream.getIdentifier());
            stream.setState(SynthesisStateEnum.FAILED.name());
            updateById(stream);
            unlockMaterials(stream.getMaterialIds(), stream.getUserId());
        }
    }

    /** SP/UR 合成完成全平台广播 */
    private void broadcastSynthesis(SynthesisStream stream) {
        try {
            SynthesisBroadcastVO msg = new SynthesisBroadcastVO();
            msg.setUserNickName(stream.getUserId());
            msg.setTargetRarity(stream.getTargetRarity());
            msg.setProductName("合成产物#" + stream.getProductHeldId());
            msg.setSerialNo(stream.getIdentifier());
            streamProducer.send("synthesisBroadcast-out-0", "SP_UR_BROADCAST", JSON.toJSONString(msg));
        } catch (Exception e) {
            log.warn("合成广播发送失败，synthesisId={}", stream.getIdentifier(), e);
        }
    }
}
