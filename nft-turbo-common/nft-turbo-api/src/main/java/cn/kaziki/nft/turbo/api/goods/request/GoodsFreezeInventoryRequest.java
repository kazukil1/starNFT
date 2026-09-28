package cn.kaziki.nft.turbo.api.goods.request;

import cn.kaziki.nft.turbo.api.goods.constant.GoodsEvent;

/**
 * 冻结库存
 */
public record GoodsFreezeInventoryRequest(String identifier, Long goodsId, Long quantity) {

    public GoodsEvent eventType() {
        return GoodsEvent.FREEZE_INVENTORY;
    }
}
