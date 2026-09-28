package cn.kaziki.nft.turbo.api.collection.request;

import cn.kaziki.nft.turbo.api.goods.constant.GoodsEvent;

/**
 * 取消藏品售卖请求
 */

public record CollectionCancelSaleRequest (String identifier, Long collectionId, Integer quantity){
    public GoodsEvent eventType(){
        return GoodsEvent.CANCEL_SALE;
    }
}
