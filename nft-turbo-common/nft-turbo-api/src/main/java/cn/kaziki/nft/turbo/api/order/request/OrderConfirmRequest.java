package cn.kaziki.nft.turbo.api.order.request;

import cn.kaziki.nft.turbo.api.goods.constant.GoodsType;
import cn.kaziki.nft.turbo.api.order.constant.TradeOrderEvent;
import lombok.Getter;
import lombok.Setter;

/**
 * 订单确认 请求
 */
@Getter
@Setter
public class OrderConfirmRequest extends BaseOrderUpdateRequest {
    // 买家Id
    private String buyerId;

    // 商品Id
    private String goodsId;

    // 商品Id
    private GoodsType goodsType;

    // 数量
    private Long itemCount;

    @Override
    public TradeOrderEvent getOrderEvent() {
        return TradeOrderEvent.CONFIRM;
    }
}

