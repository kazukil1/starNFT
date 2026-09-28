package cn.kaziki.nft.turbo.api.goods.request;

import cn.kaziki.nft.turbo.api.goods.constant.GoodsEvent;


public record GoodsUnfreezeAndSaleRequest(String identifier, Long goodsId, Long quantity) {

    public GoodsEvent eventType() {
        return GoodsEvent.UNFREEZE_AND_SALE;
    }
}
