package cn.kaziki.nft.turbo.api.goods.request;

import cn.kaziki.nft.turbo.api.goods.constant.GoodsEvent;

public record GoodsUnfreezeInventoryRequest(String identifier, Long goodsId, Long quantity) {

    public GoodsEvent eventType() {
        return GoodsEvent.UNFREEZE_INVENTORY;
    }
}
