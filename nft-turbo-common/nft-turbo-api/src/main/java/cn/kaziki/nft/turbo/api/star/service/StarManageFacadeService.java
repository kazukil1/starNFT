package cn.kaziki.nft.turbo.api.star.service;

import cn.kaziki.nft.turbo.api.star.request.StarCreateRequest;
import cn.kaziki.nft.turbo.api.star.request.StarModifyInventoryRequest;
import cn.kaziki.nft.turbo.api.star.request.StarModifyRequest;
import cn.kaziki.nft.turbo.api.star.request.StarRemoveRequest;
import cn.kaziki.nft.turbo.base.response.SingleResponse;

/**
 * 星尘闪购管理门面服务（Dubbo 接口）
 */
public interface StarManageFacadeService {

    // 创建星尘包，返回 id
    SingleResponse<Long> create(StarCreateRequest request);

    // 修改星尘包基本信息，返回 id
    SingleResponse<Long> modify(StarModifyRequest request);

    // 修改星尘包库存（增量 ±），返回 id
    SingleResponse<Long> modifyInventory(StarModifyInventoryRequest request);

    // 下架闪购包
    SingleResponse<Boolean> remove(StarRemoveRequest request);

    // 每日库存初始化（XXL-Job）
    void initTodayInventory();
}
