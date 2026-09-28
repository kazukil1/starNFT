package cn.kaziki.nft.turbo.order.domain.listener.event;

import cn.kaziki.nft.turbo.api.order.request.BaseOrderRequest;
import org.springframework.context.ApplicationEvent;

/**
 * 订单超时事件
 */
public class OrderTimeoutEvent extends ApplicationEvent {

    public OrderTimeoutEvent(BaseOrderRequest baseOrderRequest) {
        super(baseOrderRequest);
    }
}
