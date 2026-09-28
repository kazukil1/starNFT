package cn.kaziki.nft.turbo.api.collection.service;

import cn.kaziki.nft.turbo.api.collection.model.AirDropStreamVO;
import cn.kaziki.nft.turbo.api.collection.model.ArtistCollectionVO;
import cn.kaziki.nft.turbo.api.collection.model.CollectionVO;
import cn.kaziki.nft.turbo.api.collection.model.HeldCollectionVO;
import cn.kaziki.nft.turbo.api.collection.request.AirDropPageQueryRequest;
import cn.kaziki.nft.turbo.api.collection.request.CollectionPageQueryRequest;
import cn.kaziki.nft.turbo.api.collection.request.HeldCollectionPageQueryRequest;
import cn.yueyu.nft.turbo.api.collection.request.*;
import cn.kaziki.nft.turbo.base.response.PageResponse;
import cn.kaziki.nft.turbo.base.response.SingleResponse;

import java.util.List;
import java.util.Map;

/**
 * 藏品门面服务
 */
public interface CollectionReadFacadeService {

    // 根据Id查询藏品（C端）
    public SingleResponse<CollectionVO> queryById(Long collectionId);

    // 根据Id查询藏品（Artist端，含持有人数）
    public SingleResponse<ArtistCollectionVO> queryByIdForArtist(Long collectionId);

    // 藏品分页查询（C端）
    public PageResponse<CollectionVO> pageQuery(CollectionPageQueryRequest request);

    // 藏品分页查询（Artist端，含持有人数）
    public PageResponse<ArtistCollectionVO> pageQueryForArtist(CollectionPageQueryRequest request);

    // 持有藏品分页查询
    public PageResponse<HeldCollectionVO> pageQueryHeldCollection(HeldCollectionPageQueryRequest request);

    // 空投列表分页查询
    public PageResponse<AirDropStreamVO> pageQueryAirDropList(AirDropPageQueryRequest request);

    // 现有持有藏品数量查询
    public SingleResponse<Long> queryHeldCollectionCount(String userId);

    // 累计持有藏品数量查询
    SingleResponse<Long> queryHeldCollectionAllCount(String userId);

    // 用户当前持仓总铸造值查询
    SingleResponse<Long> queryUserForgeValue(String userId);

    // 根据id查询持有藏品
    public SingleResponse<HeldCollectionVO> queryHeldCollectionById(Long heldCollectionId);

    // 查询系列下藏品数量
    SingleResponse<Long> countCollectionsBySeriesId(Long seriesId);

    // 批量查询系列下藏品数量（key=seriesId, value=count）
    SingleResponse<Map<Long, Long>> countCollectionsBySeriesIds(List<Long> seriesIds);

    // 批量查询持有藏品（by ID列表）
    SingleResponse<List<HeldCollectionVO>> batchQueryHeldCollections(List<Long> ids);

    // 查询合成目标藏品（同系列+目标稀有度+SUCCEED+含SYNTHESIS途径+有库存）
    SingleResponse<CollectionVO> querySynthesisTarget(Long seriesId, String targetRarity);

    // 按用户ID查询所有持有藏品
    SingleResponse<List<HeldCollectionVO>> listHeldCollectionsByUser(String userId);

    // 按系列ID+状态查询藏品列表
    SingleResponse<List<CollectionVO>> listCollectionsBySeries(Long seriesId, String state);

    // 按ID列表批量查询藏品
    SingleResponse<List<CollectionVO>> batchQueryCollections(List<Long> ids);

}
