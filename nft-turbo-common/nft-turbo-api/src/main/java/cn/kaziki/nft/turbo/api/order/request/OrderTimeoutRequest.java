package cn.kaziki.nft.turbo.api.order.request;

import cn.kaziki.nft.turbo.api.order.constant.TradeOrderEvent;
import lombok.Getter;
import lombok.Setter;

/**
 * 订单超时 请求
 */
@Getter
@Setter
public class OrderTimeoutRequest extends BaseOrderUpdateRequest {

    @Override
    public TradeOrderEvent getOrderEvent() {
        return TradeOrderEvent.TIME_OUT;
    }
}

