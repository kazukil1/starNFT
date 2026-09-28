package cn.kaziki.nft.turbo.collection.domain.service;

import cn.kaziki.nft.turbo.api.collection.request.*;
import cn.kaziki.nft.turbo.api.goods.request.*;
import cn.yueyu.nft.turbo.api.collection.request.*;
import cn.kaziki.nft.turbo.api.collection.response.CollectionAirdropResponse;
import cn.kaziki.nft.turbo.api.collection.response.CollectionInventoryModifyResponse;
import cn.yueyu.nft.turbo.api.goods.request.*;
import cn.kaziki.nft.turbo.base.response.PageResponse;
import cn.kaziki.nft.turbo.collection.domain.entity.Collection;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 藏品服务
 */

public interface CollectionService extends IService<Collection> {

    // 创建藏品
    Collection create(CollectionCreateRequest request);

    // 下架藏品
    Boolean remove(CollectionRemoveRequest request);

    // 修改藏品价格
    Boolean modifyPrice(CollectionModifyPriceRequest request);

    // 藏品评定（稀有度/系列/铸造值/获取途径）
    Boolean assess(CollectionAssessRequest request);

    // 修改藏品库存
    CollectionInventoryModifyResponse modifyInventory(CollectionModifyInventoryRequest request);

    // 售卖
    Boolean sale(GoodsTrySaleRequest goodsTrySaleRequest);
    Boolean saleWithoutHint(GoodsTrySaleRequest goodsTrySaleRequest);

    // 冻结库存
    Boolean freezeInventory(GoodsFreezeInventoryRequest request);

    // 解冻库存
    Boolean unfreezeInventory(GoodsUnfreezeInventoryRequest request);
    Boolean unfreezeAndSale(GoodsUnfreezeAndSaleRequest request);

    // 取消--库存退还
    Boolean cancel(GoodsCancelSaleRequest request);

    // 空投
    CollectionAirdropResponse airDrop(CollectionAirDropRequest request, Collection collection);

    // 分页查询藏品列表（creatorId 可空，空则查全部；obtainType 可空，非空则 LIKE 匹配；rarity 可空，非空则精确匹配）
    PageResponse<Collection> pageQueryByState(String keyWord, String state, int currentPage, int pageSize, String creatorId, Long seriesId, String obtainType, String rarity);

    // 根据id查询藏品
    Collection queryById(Long collectionId);

    // 驳回藏品（只更新状态，不写流水）
    Boolean rejectCollection(CollectionStateChangeRequest request);

    // 重新提交审核（INIT → PENDING_REVIEW，不写流水）
    Boolean resubmitCollection(CollectionStateChangeRequest request);

}
