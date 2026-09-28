package cn.kaziki.nft.turbo.api.box.service;

import cn.kaziki.nft.turbo.api.box.request.BlindBoxCreateRequest;
import cn.kaziki.nft.turbo.api.box.request.BlindBoxModifyInventoryRequest;
import cn.kaziki.nft.turbo.api.box.request.BlindBoxModifyRequest;
import cn.kaziki.nft.turbo.api.box.response.BlindBoxCreateResponse;
import cn.kaziki.nft.turbo.base.response.BaseResponse;

/**
 * 盲盒管理门面 服务
 */
public interface BlindBoxManageFacadeService {

    /**
     * 创建盲盒
     */
    BlindBoxCreateResponse create(BlindBoxCreateRequest request);

    /**
     * 审核通过（PENDING_REVIEW → SUCCEED）
     */
    BaseResponse approve(Long boxId);

    /**
     * 驳回（PENDING_REVIEW → INIT）
     */
    BaseResponse reject(Long boxId);

    /**
     * 补充库存（追加 BlindBoxItem）
     */
    BaseResponse modifyInventory(BlindBoxModifyInventoryRequest request);

    /**
     * 修改盲盒基本信息（仅 INIT/PENDING_REVIEW 状态可修改）
     */
    BaseResponse modify(BlindBoxModifyRequest request);
}
