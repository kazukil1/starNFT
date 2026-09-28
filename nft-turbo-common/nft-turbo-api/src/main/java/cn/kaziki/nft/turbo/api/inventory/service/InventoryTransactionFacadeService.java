package cn.kaziki.nft.turbo.api.inventory.service;

import cn.kaziki.nft.turbo.api.inventory.request.InventoryRequest;

/**
 * 库存服务 tcc事务
 */
public interface InventoryTransactionFacadeService {

    // 库存扣减-try
    public Boolean tryDecrease(InventoryRequest inventoryRequest);

    // 库存扣减-confirm
    public Boolean confirmDecrease(InventoryRequest inventoryRequest);

    // 库存扣减-cancel
    public Boolean cancelDecrease(InventoryRequest inventoryRequest);
}
