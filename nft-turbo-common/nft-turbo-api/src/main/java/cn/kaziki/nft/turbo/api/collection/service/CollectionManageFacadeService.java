package cn.kaziki.nft.turbo.api.collection.service;

import cn.kaziki.nft.turbo.api.collection.model.HeldCollectionVO;
import cn.kaziki.nft.turbo.api.collection.request.*;
import cn.yueyu.nft.turbo.api.collection.request.*;
import cn.kaziki.nft.turbo.api.collection.response.CollectionAirdropResponse;
import cn.kaziki.nft.turbo.api.collection.response.CollectionCreateResponse;
import cn.kaziki.nft.turbo.api.collection.response.CollectionModifyResponse;
import cn.kaziki.nft.turbo.api.collection.response.CollectionRemoveResponse;
import cn.kaziki.nft.turbo.base.response.SingleResponse;

import java.util.List;

/**
 * 藏品管理门面服务
 */
public interface CollectionManageFacadeService {

    /**
     * 创建藏品
     *
     * @param request
     * @return
     */
    CollectionCreateResponse create(CollectionCreateRequest request);

    /**
     * 审核通过（上链 + 状态变更）
     *
     * @param request
     * @return
     */
    CollectionModifyResponse approveCollection(CollectionStateChangeRequest request);


    /**
     * 藏品下架
     *
     * @param request
     * @return
     */
    CollectionRemoveResponse remove(CollectionRemoveRequest request);


    /**
     * 藏品库存修改
     *
     * @param request
     * @return
     */
    CollectionModifyResponse modifyInventory(CollectionModifyInventoryRequest request);

    /**
     * 藏品价格修改
     *
     * @param request
     * @return
     */
    CollectionModifyResponse modifyPrice(CollectionModifyPriceRequest request);

    /**
     * 藏品评定（稀有度/系列/铸造值/获取途径）
     *
     * @param request
     * @return
     */
    CollectionModifyResponse assessCollection(CollectionAssessRequest request);

    /**
     * 空投
     * @param request
     * @return
     */
    CollectionAirdropResponse airDrop(CollectionAirDropRequest request);

    /**
     * 驳回藏品（PENDING_REVIEW → INIT）
     * @param request
     * @return
     */
    CollectionModifyResponse rejectCollection(CollectionStateChangeRequest request);

    /**
     * 重新提交审核（INIT → PENDING_REVIEW）
     * @param request
     * @return
     */
    CollectionModifyResponse resubmitCollection(CollectionStateChangeRequest request);

    /** 创建持有藏品（供合成/开盒等跨模块调用） */
    SingleResponse<HeldCollectionVO> createHeldCollection(HeldCollectionCreateDTO dto);

    /** 批量更新持有藏品状态（lock/unlock/destroy，校验userId归属） */
    SingleResponse<Boolean> batchUpdateHeldCollectionState(List<Long> ids, String newState, String userId);
}
