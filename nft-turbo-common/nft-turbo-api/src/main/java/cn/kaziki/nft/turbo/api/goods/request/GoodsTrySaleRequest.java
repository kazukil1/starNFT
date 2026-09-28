package cn.kaziki.nft.turbo.api.goods.request;

import cn.kaziki.nft.turbo.api.goods.constant.GoodsEvent;


public record GoodsTrySaleRequest(String identifier, Long goodsId, Long quantity, String extendInfo) {

    public GoodsEvent eventType() {
        return GoodsEvent.TRY_SALE;
    }
}
