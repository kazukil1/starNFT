package cn.kaziki.nft.turbo.api.check.service;

import cn.kaziki.nft.turbo.api.check.request.InventoryCheckRequest;
import cn.kaziki.nft.turbo.api.check.response.InventoryCheckResponse;

/**
 * 库存检查 rpc服务
 */
public interface InventoryCheckFacadeService {

    // 库存核对
    public InventoryCheckResponse check(InventoryCheckRequest request);
}
