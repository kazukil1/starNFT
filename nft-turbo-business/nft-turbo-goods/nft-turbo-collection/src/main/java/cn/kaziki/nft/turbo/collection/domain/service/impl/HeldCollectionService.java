package cn.kaziki.nft.turbo.collection.domain.service.impl;

import cn.kaziki.nft.turbo.api.collection.model.ForgeRankVO;
import cn.kaziki.nft.turbo.api.album.event.AlbumRefreshEvent;
import cn.kaziki.nft.turbo.api.collection.constant.GoodsSaleBizType;
import cn.kaziki.nft.turbo.api.collection.constant.HeldCollectionState;
import cn.kaziki.nft.turbo.api.collection.model.HeldCollectionDTO;
import cn.kaziki.nft.turbo.api.collection.request.HeldCollectionPageQueryRequest;
import cn.kaziki.nft.turbo.base.response.PageResponse;
import cn.kaziki.nft.turbo.cache.constant.CacheConstant;
import cn.kaziki.nft.turbo.collection.domain.constant.HeldCollectionEventType;
import cn.kaziki.nft.turbo.collection.domain.entity.HeldCollection;
import cn.kaziki.nft.turbo.collection.domain.entity.HeldCollectionStream;
import cn.kaziki.nft.turbo.collection.domain.entity.convertor.HeldCollectionConvertor;
import cn.kaziki.nft.turbo.collection.domain.request.HeldCollectionActiveRequest;
import cn.kaziki.nft.turbo.collection.domain.request.HeldCollectionCreateRequest;
import cn.kaziki.nft.turbo.collection.domain.request.HeldCollectionDestroyRequest;
import cn.kaziki.nft.turbo.collection.domain.request.HeldCollectionTransferRequest;
import cn.kaziki.nft.turbo.collection.exception.CollectionErrorCode;
import cn.kaziki.nft.turbo.collection.exception.CollectionException;
import cn.kaziki.nft.turbo.collection.infrastructure.mapper.CollectionMapper;
import cn.kaziki.nft.turbo.collection.infrastructure.mapper.HeldCollectionMapper;
import cn.kaziki.nft.turbo.lock.DistributeLock;
import cn.kaziki.turbo.stream.producer.StreamProducer;
import cn.hutool.core.lang.Assert;
import org.springframework.context.ApplicationEventPublisher;
import com.alibaba.fastjson2.JSON;
import com.alicp.jetcache.anno.CacheRefresh;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.CacheInvalidate;
import com.alicp.jetcache.anno.Cached;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.collections4.CollectionUtils;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.protocol.ScoredEntry;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static cn.kaziki.nft.turbo.collection.exception.CollectionErrorCode.*;

/**
 * 持有藏品的服务
 */

@Service
public class HeldCollectionService extends ServiceImpl<HeldCollectionMapper, HeldCollection>
        implements InitializingBean {

    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private HeldCollectionStreamService heldCollectionStreamService;
    @Autowired
    private StreamProducer streamProducer;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private CollectionMapper collectionMapper;
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    private static final String HELD_COLLECTION_BIND_BOX_PREFIX = "HC:SALES:";

    private RScoredSortedSet<String> forgeRank;

    @Override
    public void afterPropertiesSet() {
        this.forgeRank = redissonClient.getScoredSortedSet("forgeRank");
    }

    // 创建持有藏品
    @Transactional(rollbackFor = Exception.class)
    @DistributeLock(keyExpression = "#request.serialNoBaseId", scene = "HELD_COLLECTION_CREATE")
    public HeldCollection create(HeldCollectionCreateRequest request) {
        // 1.查重
        HeldCollection existHeldCollection = queryByCollectionIdAndBizNo(request.getGoodsId(), request.getBizNo());
        if (existHeldCollection != null) {
            return existHeldCollection;
        }

        //HC:SALES:COLLECTION:1234 or HC:SALES:BIND_BOX:1234
        HeldCollection heldCollection = new HeldCollection();
        Long serialNo = redissonClient.getAtomicLong(HELD_COLLECTION_BIND_BOX_PREFIX + request.getGoodsType() + CacheConstant.CACHE_KEY_SEPARATOR + request.getSerialNoBaseId()).incrementAndGet();

        try {
            // 2.创建持有藏品（init）
            heldCollection.init(request, serialNo.toString());
            var saveResult = this.save(heldCollection);
            if (!saveResult) {
                throw new CollectionException(HELD_COLLECTION_SAVE_FAILED);
            }

            // 3.新增持有藏品流水
            HeldCollectionStream heldCollectionStream = new HeldCollectionStream().generateForCreate(heldCollection.getId(), request.getIdentifier());
            saveResult = heldCollectionStreamService.save(heldCollectionStream);
            Assert.isTrue(saveResult, () -> new CollectionException(HELD_COLLECTION_STREAM_SAVE_FAILED));

            // 图鉴进度刷新（异步，不阻塞主流程）
            publishAlbumRefreshEvent(request.getUserId(), request.getGoodsId());

            return heldCollection;
        } catch (Throwable throwable) {
            // 4.如果抛了异常，并且数据库未更新成功过，则回滚序列号
            heldCollection = queryByCollectionIdAndBizNo(request.getGoodsId(), request.getBizNo());
            if (heldCollection == null) {
                redissonClient.getAtomicLong(HELD_COLLECTION_BIND_BOX_PREFIX + request.getGoodsType() + CacheConstant.CACHE_KEY_SEPARATOR + request.getSerialNoBaseId()).decrementAndGet();
                return null;
            }
            return heldCollection;
        }
    }



    // 根据藏品id和藏品编号查询藏品
    public HeldCollection queryByCollectionIdAndBizNo(Long collectionId, String bizNo) {
        QueryWrapper<HeldCollection> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("collection_id", collectionId);
        queryWrapper.eq("biz_no", bizNo);
        List<HeldCollection> retList = list(queryWrapper);
        if (CollectionUtils.isEmpty(retList)) {
            return null;
        }
        return retList.get(0);
    }

    // 根据持有藏品状态查询藏品
    public PageResponse<HeldCollection> pageQueryBySteate(HeldCollectionPageQueryRequest request) {
        Page<HeldCollection> page = new Page<>(request.getCurrentPage(),request.getPageSize());
        QueryWrapper<HeldCollection> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id",request.getUserId());
        if(request.getKeyword() != null){
            wrapper.like("name",request.getKeyword());
        }
        if(request.getState() != null){
            wrapper.eq("state",request.getState());
        }
        wrapper.orderBy(true, false, "gmt_create");
        Page<HeldCollection> collectionPage = page(page, wrapper);
        return PageResponse.of(collectionPage.getRecords(), (int) collectionPage.getTotal(),
                request.getPageSize(),request.getCurrentPage());
    }

    // 查询现有持有藏品数量
    public long queryHeldCollectionCount(String userId) {
        QueryWrapper wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.eq("state", HeldCollectionState.ACTIVED.toString());
        return this.count(wrapper);
    }

    // 查询累计持有藏品数量
    public Long queryHeldCollectionAllCount(String userId) {
        QueryWrapper wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        return this.count(wrapper);
    }

    // 激活持有藏品
    @CacheInvalidate(name = ":held_collection:cache:id:", key = "#request.heldCollectionId")
    public Boolean active(HeldCollectionActiveRequest request) {
        HeldCollection heldCollection = getById(request.getHeldCollectionId());
        if (null == heldCollection) {
            throw new CollectionException(HELD_COLLECTION_QUERY_FAIL);
        }

        if (heldCollection.getState().equals(HeldCollectionState.ACTIVED)) {
            return true;
        }

        heldCollection.actived(request.getNftId(), request.getTxHash());
        HeldCollectionStream heldCollectionStream = new HeldCollectionStream().generateForActive(heldCollection.getId(), request.getIdentifier());

        //用编程式事务代替声明式事务，避免后面MQ发送超时，导致事务回滚
        transactionTemplate.executeWithoutResult(status -> {
            boolean result = updateById(heldCollection);
            Assert.isTrue(result, () -> new CollectionException(HELD_COLLECTION_SAVE_FAILED));
            boolean saveResult = heldCollectionStreamService.save(heldCollectionStream);
            Assert.isTrue(saveResult, () -> new CollectionException(HELD_COLLECTION_STREAM_SAVE_FAILED));
        });

        if (heldCollection.getBizType() != GoodsSaleBizType.AIR_DROP) {
            sendMsg(heldCollection, request.getEventType());
        }
        // 更新铸造值排行榜
        updateForgeRank(heldCollection.getUserId());
        return true;
    }

    private boolean sendMsg(HeldCollection heldCollection, HeldCollectionEventType eventType) {
        HeldCollectionDTO heldCollectionDTO = HeldCollectionConvertor.INSTANCE.mapToDto(heldCollection);
        //消息监听：HeldCollectionMsgListener
        return streamProducer.send("heldCollection-out-0", eventType.name(), JSON.toJSONString(heldCollectionDTO));
    }

    // 用户持有藏品详情
    @Cached(name = ":held_collection:cache:id:", expire = 60, localExpire = 10, timeUnit = TimeUnit.MINUTES, cacheType = CacheType.BOTH, key = "#heldCollectionId", cacheNullValue = true)
    @CacheRefresh(refresh = 50, timeUnit = TimeUnit.MINUTES)
    public HeldCollection queryById(Long heldCollectionId) {
        return getById(heldCollectionId);
    }

    public long countHolderByCollectionId(Long collectionId) {
        return this.baseMapper.countHolderByCollectionId(collectionId);
    }

    // 用户当前持仓总铸造值
    public Long sumForgeValueByUserId(String userId) {
        return this.baseMapper.sumForgeValueByUserId(userId);
    }

    // 藏品转让
    @Transactional(rollbackFor = Exception.class)
    @CacheInvalidate(name = ":held_collection:cache:id:", key = "#request.heldCollectionId")
    public HeldCollection transfer(HeldCollectionTransferRequest request) {
        HeldCollection oldHeldCollection = getById(request.getHeldCollectionId());
        preCheckForTransfer(request, oldHeldCollection);

        // 1.更新原持有藏品状态invalid
        boolean res = updateById(oldHeldCollection.inActived());
        Assert.isTrue(res, () -> new CollectionException(HELD_COLLECTION_SAVE_FAILED));

        // 2.新增持有藏品流水
        HeldCollectionStream transferOutStream = new HeldCollectionStream().generateForTransferOut(oldHeldCollection.getId(),
                request.getIdentifier(), oldHeldCollection.getUserId());
        res = heldCollectionStreamService.save(transferOutStream);
        Assert.isTrue(res, () -> new CollectionException(HELD_COLLECTION_STREAM_SAVE_FAILED));

        // 3.生成新持有藏品
        HeldCollection newHeldCollection = new HeldCollection().transfer(oldHeldCollection, request.getRecipientUserId());
        res = save(newHeldCollection);
        Assert.isTrue(res, () -> new CollectionException(HELD_COLLECTION_SAVE_FAILED));
        HeldCollectionStream transferInStream = new HeldCollectionStream().generateForTransferIn(newHeldCollection.getId(),
                request.getIdentifier(), newHeldCollection.getUserId());

        // 4.新增持有藏品流水
        res = heldCollectionStreamService.save(transferInStream);
        Assert.isTrue(res, () -> new CollectionException(HELD_COLLECTION_STREAM_SAVE_FAILED));

        // 接收方图鉴进度刷新
        publishAlbumRefreshEvent(request.getRecipientUserId(), oldHeldCollection.getCollectionId());

        // 更新铸造值排行榜（转出方 + 转入方）
        updateForgeRank(oldHeldCollection.getUserId());
        updateForgeRank(newHeldCollection.getUserId());

        return newHeldCollection;
    }

    private void preCheckForTransfer(HeldCollectionTransferRequest request, HeldCollection oldHledCollection) {
        if(oldHledCollection == null){
            throw new CollectionException(HELD_COLLECTION_QUERY_FAIL);
        }
        if(oldHledCollection.getState() != HeldCollectionState.ACTIVED){
            throw new CollectionException(HELD_COLLECTION_STATE_CHECK_ERROR);
        }
        if(!oldHledCollection.getUserId().equals(request.getOperatorId())){
            throw new CollectionException(HELD_COLLECTION_OWNER_CHECK_ERROR);
        }
    }

    // 销毁持有藏品
    @Transactional(rollbackFor = Exception.class)
    @CacheInvalidate(name = ":held_collection:cache:id:", key = "#request.heldCollectionId")
    public HeldCollection destroy(HeldCollectionDestroyRequest request) {
        HeldCollection heldCollection = this.getById(request.getHeldCollectionId());
        // 1.前置校验
        preCheckForDestroy(request, heldCollection);

        if (heldCollection.getState() == HeldCollectionState.DESTROYING || heldCollection.getState() == HeldCollectionState.DESTROYED) {
            return heldCollection;
        }

        // 2.更新持有藏品状态destroying
        heldCollection.destroying();
        boolean result = this.updateById(heldCollection);

        // 3.新增持有藏品流水
        HeldCollectionStream heldCollectionStream = new HeldCollectionStream().generateForDestroy(heldCollection.getId(), request.getIdentifier(), request.getOperatorId());
        Assert.isTrue(result, () -> new CollectionException(HELD_COLLECTION_SAVE_FAILED));
        boolean saveResult = heldCollectionStreamService.save(heldCollectionStream);
        Assert.isTrue(saveResult, () -> new CollectionException(HELD_COLLECTION_STREAM_SAVE_FAILED));

        // 更新铸造值排行榜
        updateForgeRank(heldCollection.getUserId());

        return heldCollection;
    }

    private static void preCheckForDestroy(HeldCollectionDestroyRequest request, HeldCollection oldHeldCollection) {
        if (oldHeldCollection == null) {
            throw new CollectionException(CollectionErrorCode.HELD_COLLECTION_QUERY_FAIL);
        }

        if (!oldHeldCollection.getUserId().equals(request.getOperatorId())) {
            throw new CollectionException(CollectionErrorCode.HELD_COLLECTION_OWNER_CHECK_ERROR);
        }
        // 合成锁定中的卡不可直接销毁（需走合成链路 chain.destroy）
        if (oldHeldCollection.getState() == HeldCollectionState.LOCKED) {
            throw new CollectionException(CollectionErrorCode.HELD_COLLECTION_STATE_CHECK_ERROR);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public List<HeldCollection> batchCreate(List<HeldCollectionCreateRequest> heldCollectionCreateRequests) {

        List<HeldCollection> heldCollections = new ArrayList<>();
        List<HeldCollectionStream> heldCollectionStreams = new ArrayList<>();
        for (HeldCollectionCreateRequest request : heldCollectionCreateRequests) {
            HeldCollection heldCollection = new HeldCollection();
            Long serialNo = redissonClient.getAtomicLong(HELD_COLLECTION_BIND_BOX_PREFIX + request.getGoodsType() + CacheConstant.CACHE_KEY_SEPARATOR + request.getSerialNoBaseId()).incrementAndGet();
            heldCollection.init(request, serialNo.toString());
            heldCollections.add(heldCollection);
        }

        //this调用会使saveBatch中的事务失效，需要在本方法外增加事务
        boolean result = this.saveBatch(heldCollections);
        Assert.isTrue(result, () -> new CollectionException(HELD_COLLECTION_SAVE_FAILED));

        for (HeldCollection heldCollection : heldCollections) {
            HeldCollectionStream heldCollectionStream = new HeldCollectionStream().generateForCreate(heldCollection.getId(), heldCollection.getId().toString());
            heldCollectionStreams.add(heldCollectionStream);
        }

        result = heldCollectionStreamService.saveBatch(heldCollectionStreams);
        Assert.isTrue(result, () -> new CollectionException(HELD_COLLECTION_STREAM_SAVE_FAILED));
        return heldCollections;
    }

    /** 发布图鉴进度刷新事件（异步，失败不阻塞主流程） */
    private void publishAlbumRefreshEvent(String userId, Long collectionId) {
        if (userId == null || collectionId == null) return;
        try {
            cn.kaziki.nft.turbo.collection.domain.entity.Collection col = collectionMapper.selectById(collectionId);
            if (col != null && col.getSeriesId() != null) {
                eventPublisher.publishEvent(new AlbumRefreshEvent(this, userId, col.getSeriesId()));
            }
        } catch (Exception ignored) {
            // 图鉴刷新失败不阻塞主流程
        }
    }

    // ==================== 铸造值排行榜 ====================

    /** 更新用户铸造值排名（铸造值变化时调用） */
    private void updateForgeRank(String userId) {
        if (userId == null) return;
        try {
            Long forgeValue = sumForgeValueByUserId(userId);
            if (forgeValue == null) forgeValue = 0L;
            // 组合 score：高位=铸造值，低位=时间戳（先到排前，取反使得越早值越大）
            double score = forgeValue.doubleValue() * 10_000_000_000L
                    + (System.currentTimeMillis() / 1000.0);
            forgeRank.add(score, userId);
        } catch (Exception e) {
            log.warn("更新铸造值排行榜失败，userId=" + userId);
        }
    }

    /** 获取铸造值排行榜 TopN */
    public List<ForgeRankVO> getForgeRankTopN(int topN) {
        List<ForgeRankVO> result = new ArrayList<>();
        try {
            Collection<ScoredEntry<String>> entries = forgeRank.entryRangeReversed(0, topN - 1);
            if (entries != null) {
                int rank = 1;
                for (ScoredEntry<String> entry : entries) {
                    String userId = entry.getValue();
                    if (userId == null) continue;
                    Long forgeValue = (long) (entry.getScore() / 10_000_000_000L);
                    ForgeRankVO vo =
                            new ForgeRankVO();
                    vo.setRank(rank++);
                    vo.setUserId(userId);
                    vo.setForgeValue(forgeValue);
                    result.add(vo);
                }
            }
        } catch (Exception e) {
            log.error("查询铸造值排行榜失败");
        }
        return result;
    }

    /** 获取当前用户铸造值排名（1-based，0 表示未上榜） */
    public Integer getMyForgeRank(String userId) {
        try {
            Integer rank = forgeRank.revRank(userId);
            return rank != null ? rank + 1 : 0;
        } catch (Exception e) {
            log.error("查询用户铸造值排名失败，userId=" + userId);
            return 0;
        }
    }
}
