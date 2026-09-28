package cn.kaziki.nft.turbo.api.goods.request;

import cn.kaziki.nft.turbo.api.goods.constant.GoodsEvent;
/**
 * 商品取消售卖 请求
 */
public record GoodsCancelSaleRequest(String identifier, Long goodsId, Long quantity, String extendInfo) {

    public GoodsEvent eventType() {
        return GoodsEvent.CANCEL_SALE;
    }
}
