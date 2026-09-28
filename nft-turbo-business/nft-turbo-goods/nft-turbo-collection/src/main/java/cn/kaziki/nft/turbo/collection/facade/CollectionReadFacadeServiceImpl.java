package cn.kaziki.nft.turbo.collection.facade;

import cn.kaziki.nft.turbo.api.collection.constant.CollectionObtainType;
import cn.kaziki.nft.turbo.api.collection.constant.CollectionStateEnum;
import cn.kaziki.nft.turbo.api.collection.model.*;
import cn.yueyu.nft.turbo.api.collection.model.*;
import cn.kaziki.nft.turbo.api.collection.request.AirDropPageQueryRequest;
import cn.kaziki.nft.turbo.api.collection.request.CollectionPageQueryRequest;
import cn.kaziki.nft.turbo.api.collection.request.HeldCollectionPageQueryRequest;
import cn.kaziki.nft.turbo.api.collection.service.CollectionReadFacadeService;
import cn.kaziki.nft.turbo.api.collection.service.SeriesReadFacadeService;
import cn.kaziki.nft.turbo.api.goods.constant.GoodsType;
import cn.kaziki.nft.turbo.api.inventory.request.InventoryRequest;
import cn.kaziki.nft.turbo.api.inventory.service.InventoryFacadeService;
import cn.kaziki.nft.turbo.base.response.PageResponse;
import cn.kaziki.nft.turbo.base.response.SingleResponse;
import cn.kaziki.nft.turbo.collection.domain.entity.Collection;
import cn.kaziki.nft.turbo.collection.domain.entity.CollectionAirdropStream;
import cn.kaziki.nft.turbo.collection.domain.entity.HeldCollection;
import cn.kaziki.nft.turbo.collection.domain.entity.convertor.CollectionAirdropStreamConvertor;
import cn.kaziki.nft.turbo.collection.domain.entity.convertor.CollectionConvertor;
import cn.kaziki.nft.turbo.collection.domain.entity.convertor.HeldCollectionConvertor;
import cn.kaziki.nft.turbo.collection.domain.service.CollectionService;
import cn.kaziki.nft.turbo.collection.domain.service.impl.HeldCollectionService;
import cn.kaziki.nft.turbo.collection.infrastructure.mapper.CollectionAirdropStreamMapper;
import cn.kaziki.nft.turbo.rpc.facade.Facade;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

import static cn.kaziki.nft.turbo.collection.exception.CollectionErrorCode.COLLECTION_NOT_EXIST;

@DubboService(version = "1.0.0")
public class CollectionReadFacadeServiceImpl implements CollectionReadFacadeService {
    @Autowired
    private CollectionService collectionService;
    @Autowired
    private HeldCollectionService heldCollectionService;
    @Autowired
    private InventoryFacadeService inventoryFacadeService;
    @Autowired
    private CollectionAirdropStreamMapper collectionAirdropStreamMapper;
    @Autowired
    private SeriesReadFacadeService seriesReadFacadeService;

    // 用户当前持仓总铸造值查询
    @Override
    public SingleResponse<Long> queryUserForgeValue(String userId) {
        return SingleResponse.of(heldCollectionService.sumForgeValueByUserId(userId));
    }

    @Override
    @Facade
    public SingleResponse<CollectionVO> queryById(Long collectionId) {
        // 1.查询藏品
        Collection collection = collectionService.queryById(collectionId);
        if(collection == null){
            return SingleResponse.fail(COLLECTION_NOT_EXIST.getCode(),COLLECTION_NOT_EXIST.getMessage());
        }
        // 2.查询藏品库存
        InventoryRequest request = new InventoryRequest();
        request.setGoodsId(collectionId.toString());
        request.setGoodsType(GoodsType.COLLECTION);
        //   从缓存获取
        SingleResponse<Integer> response = inventoryFacadeService.queryInventory(request);
        //   数据库中的库存
        Integer inventory = collection.getSaleableInventory().intValue();
        if(response.getSuccess()){
            // 如果从缓存查询成功，则用缓存的值，否则用数据库的值（扣减库存先经过缓存再修改数据库）
            inventory = response.getData();
        }

        // 组装VO
        CollectionVO collectionVO = CollectionConvertor.INSTANCE.mapToVo(collection);
        collectionVO.setCollectionState(collection.getState().name());
        collectionVO.setInventory(Long.valueOf(inventory));
        collectionVO.setState(collection.getState(), collection.getSaleTime(), inventory.longValue());
        // 补系列名（seriesId 由 Convertor 自动映射；系列查询走两级缓存）
        if (collection.getSeriesId() != null) {
            SeriesVO series = seriesReadFacadeService.queryById(collection.getSeriesId()).getData();
            collectionVO.setSeriesName(series == null ? null : series.getName());
        }
        // 补持有人数
        collectionVO.setHolderCount(heldCollectionService.countHolderByCollectionId(collectionId));
        return SingleResponse.of(collectionVO);
    }

    // 分页查询（C端传 obtainType=DIRECT_SALE 过滤，Admin 不传则不过滤）
    @Override
    public PageResponse<CollectionVO> pageQuery(CollectionPageQueryRequest request) {
        PageResponse<Collection> collectionPage = collectionService.pageQueryByState(
                request.getKeyword(), request.getState(), request.getCurrentPage(), request.getPageSize(),
                request.getCreatorId(), request.getSeriesId(),
                request.getObtainType(), request.getRarity()
        );
        List<CollectionVO> vos = CollectionConvertor.INSTANCE.mapToVo(collectionPage.getDatas());
        for (CollectionVO vo : vos) {
            vo.setSeriesName(seriesReadFacadeService.queryById(vo.getSeriesId()).getData().getName());
        }
        return PageResponse.of(vos, collectionPage.getTotal(),collectionPage.getPageSize(),collectionPage.getCurrentPage());
    }


    // Artist端分页查询（含持有人数，obtainType 传 null 则不过滤）
    @Override
    public PageResponse<ArtistCollectionVO> pageQueryForArtist(CollectionPageQueryRequest request) {
        PageResponse<Collection> collectionPage = collectionService.pageQueryByState(
                request.getKeyword(), request.getState(), request.getCurrentPage(), request.getPageSize(),
                request.getCreatorId(), request.getSeriesId(),
                request.getObtainType(), request.getRarity()
        );
        var collectionVOList = CollectionConvertor.INSTANCE.mapToArtistVo(collectionPage.getDatas());
        // 批量填充持有人数和系列名
        Set<Long> seriesIds = collectionVOList.stream()
                .map(ArtistCollectionVO::getSeriesId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, String> seriesNameMap = new HashMap<>();
        for (Long seriesId : seriesIds) {
            SeriesVO series = seriesReadFacadeService.queryById(seriesId).getData();
            seriesNameMap.put(seriesId, series == null ? null : series.getName());
        }
        for (ArtistCollectionVO collection : collectionVOList) {
            collection.setHolderCount(heldCollectionService.countHolderByCollectionId(collection.getId()));
            if (collection.getSeriesId() != null) {
                collection.setSeriesName(seriesNameMap.get(collection.getSeriesId()));
            }
        }
        return PageResponse.of(collectionVOList, collectionPage.getTotal(), collectionPage.getPageSize(), collectionPage.getCurrentPage());
    }

    // Artist端根据id查询（含持有人数）
    @Override
    public SingleResponse<ArtistCollectionVO> queryByIdForArtist(Long collectionId) {
        Collection collection = collectionService.queryById(collectionId);
        if (collection == null) {
            return SingleResponse.fail(COLLECTION_NOT_EXIST.getCode(), COLLECTION_NOT_EXIST.getMessage());
        }
        InventoryRequest request = new InventoryRequest();
        request.setGoodsId(collectionId.toString());
        request.setGoodsType(GoodsType.COLLECTION);
        SingleResponse<Integer> response = inventoryFacadeService.queryInventory(request);
        Integer inventory = collection.getSaleableInventory().intValue();
        if (response.getSuccess()) {
            inventory = response.getData();
        }

        ArtistCollectionVO vo = CollectionConvertor.INSTANCE.mapToArtistVo(collection);
        vo.setCollectionState(collection.getState().name());
        vo.setInventory(Long.valueOf(inventory));
        vo.setState(collection.getState(), collection.getSaleTime(), inventory.longValue());
        vo.setHolderCount(heldCollectionService.countHolderByCollectionId(collectionId));
        if (collection.getSeriesId() != null) {
            SeriesVO seriesVO = seriesReadFacadeService.queryById(collection.getSeriesId()).getData();
            vo.setSeriesName(seriesVO == null ? null : seriesVO.getName());
        }
        return SingleResponse.of(vo);
    }

    @Override
    public PageResponse<HeldCollectionVO> pageQueryHeldCollection(HeldCollectionPageQueryRequest request) {
        PageResponse<HeldCollection> pageResponse = heldCollectionService.pageQueryBySteate(request);
        List<HeldCollectionVO> vos = HeldCollectionConvertor.INSTANCE.mapToVo(pageResponse.getDatas());
        for(HeldCollectionVO vo : vos){
            Long seriesId = collectionService.queryById(vo.getCollectionId()).getSeriesId();
            vo.setSeriesName(seriesReadFacadeService.queryById(seriesId).getData().getName());
        }
        return PageResponse.of(vos, pageResponse.getTotal(), pageResponse.getPageSize(), pageResponse.getCurrentPage());
    }

    @Override
    public PageResponse<AirDropStreamVO> pageQueryAirDropList(AirDropPageQueryRequest request) {
        Page<CollectionAirdropStream> page = new Page<>(request.getCurrentPage(), request.getPageSize());
        QueryWrapper<CollectionAirdropStream> wrapper = new QueryWrapper<>();
        if (request.getCollectionId() != null) {
            wrapper.eq("collection_id", request.getCollectionId());
        }

        if (request.getUserId() != null) {
            wrapper.eq("recipient_user_id", request.getUserId());
        }
        wrapper.orderByDesc("gmt_create");
        Page<CollectionAirdropStream> collectionAirdropStreamPage = collectionAirdropStreamMapper.selectPage(page, wrapper);
        return PageResponse.of(CollectionAirdropStreamConvertor.INSTANCE.mapToVo(collectionAirdropStreamPage.getRecords()), (int) collectionAirdropStreamPage.getTotal(), request.getPageSize(), request.getCurrentPage());
    }

    // 查询现有持有藏品数量
    @Override
    public SingleResponse<Long> queryHeldCollectionCount(String userId) {
        return SingleResponse.of(heldCollectionService.queryHeldCollectionCount(userId));
    }

    // 查询累计持有藏品数量
    @Override
    public SingleResponse<Long> queryHeldCollectionAllCount(String userId) {
        return SingleResponse.of(heldCollectionService.queryHeldCollectionAllCount(userId));
    }

    // 用户持有藏品详情
    @Override
    public SingleResponse<HeldCollectionVO> queryHeldCollectionById(Long heldCollectionId) {
        HeldCollection heldCollection = heldCollectionService.queryById(heldCollectionId);
        return SingleResponse.of(HeldCollectionConvertor.INSTANCE.mapToVo(heldCollection));
    }

    @Override
    public SingleResponse<Long> countCollectionsBySeriesId(Long seriesId) {
        long count = collectionService.count(new QueryWrapper<Collection>().eq("series_id", seriesId));
        return SingleResponse.of(count);
    }

    @Override
    public SingleResponse<Map<Long, Long>> countCollectionsBySeriesIds(List<Long> seriesIds) {
        if (seriesIds == null || seriesIds.isEmpty()) {
            return SingleResponse.of(java.util.Map.of());
        }
        QueryWrapper<Collection> wrapper = new QueryWrapper<>();
        wrapper.select("series_id", "count(*) as cnt")
                .in("series_id", seriesIds)
                .groupBy("series_id");
        Map<Long, Long> countMap = collectionService.listMaps(wrapper).stream()
                .collect(java.util.stream.Collectors.toMap(
                        m -> Long.valueOf(m.get("series_id").toString()),
                        m -> Long.valueOf(m.get("cnt").toString())));
        return SingleResponse.of(countMap);
    }

    // 批量查询持有藏品
    @Override
    public SingleResponse<List<HeldCollectionVO>> batchQueryHeldCollections(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return SingleResponse.of(java.util.Collections.emptyList());
        }
        List<HeldCollection> list = heldCollectionService.listByIds(ids);
        return SingleResponse.of(HeldCollectionConvertor.INSTANCE.mapToVo(list));
    }

    // 查询合成目标藏品（同系列+目标稀有度+SUCCEED+含SYNTHESIS途径+有库存）
    @Override
    public SingleResponse<CollectionVO> querySynthesisTarget(Long seriesId, String targetRarity) {
        QueryWrapper<Collection> wrapper = new QueryWrapper<>();
        wrapper.eq("series_id", seriesId);
        wrapper.eq("rarity", targetRarity);
        wrapper.eq("state", CollectionStateEnum.SUCCEED);
        wrapper.like("obtain_type", CollectionObtainType.SYNTHESIS.name());
        wrapper.gt("saleable_inventory", 0);
        List<Collection> candidates = collectionService.list(wrapper);
        if (candidates == null || candidates.isEmpty()) {
            return SingleResponse.fail("SYNTHESIS_TARGET_SOLD_OUT", "合成目标藏品不存在或已售罄");
        }
        // 随机选取：有N个候选则每个概率 1/N
        int index = java.util.concurrent.ThreadLocalRandom.current().nextInt(candidates.size());
        Collection target = candidates.get(index);
        return SingleResponse.of(CollectionConvertor.INSTANCE.mapToVo(target));
    }

    // 按用户ID查询所有持有藏品
    @Override
    public SingleResponse<List<HeldCollectionVO>> listHeldCollectionsByUser(String userId) {
        QueryWrapper<HeldCollection> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        List<HeldCollection> list = heldCollectionService.list(wrapper);
        return SingleResponse.of(HeldCollectionConvertor.INSTANCE.mapToVo(list));
    }

    // 按ID列表批量查询藏品
    @Override
    public SingleResponse<List<CollectionVO>> batchQueryCollections(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return SingleResponse.of(java.util.Collections.emptyList());
        }
        List<Collection> list = collectionService.listByIds(ids);
        return SingleResponse.of(CollectionConvertor.INSTANCE.mapToVo(list));
    }

    // 按系列ID+状态查询藏品列表
    @Override
    public SingleResponse<List<CollectionVO>> listCollectionsBySeries(Long seriesId, String state) {
        QueryWrapper<Collection> wrapper = new QueryWrapper<>();
        wrapper.eq("series_id", seriesId);
        if (state != null) {
            wrapper.eq("state", state);
        }
        List<Collection> list = collectionService.list(wrapper);
        return SingleResponse.of(CollectionConvertor.INSTANCE.mapToVo(list));
    }
}
