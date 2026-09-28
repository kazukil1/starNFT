package cn.kaziki.nft.turbo.api.collection.request;

import cn.kaziki.nft.turbo.api.goods.constant.GoodsEvent;

import java.math.BigDecimal;

public record CollectionConfirmSaleRequest (String identifier, Long collectionId, Long quantity,
                                            String bizNo, String bizType, String userId,
                                            String name, String cover, BigDecimal purchasePrice){
    public GoodsEvent eventType(){
        return GoodsEvent.CONFIRM_SALE;

    }
}
