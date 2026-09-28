package cn.kaziki.nft.turbo.order.domain.listener.event;

import cn.kaziki.nft.turbo.order.domain.entity.TradeOrder;
import org.springframework.context.ApplicationEvent;

/**
 * 订单创建事件
 */
public class OrderCreateEvent extends ApplicationEvent {

    public OrderCreateEvent(TradeOrder tradeOrder) {
        super(tradeOrder);
    }
}
