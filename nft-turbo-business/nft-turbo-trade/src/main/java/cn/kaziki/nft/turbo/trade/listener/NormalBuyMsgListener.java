package cn.kaziki.nft.turbo.trade.listener;

import cn.kaziki.nft.turbo.api.goods.request.GoodsSaleRequest;
import cn.kaziki.nft.turbo.api.goods.service.GoodsTransactionFacadeService;
import cn.kaziki.nft.turbo.api.order.constant.TradeOrderState;
import cn.kaziki.nft.turbo.api.order.model.TradeOrderVO;
import cn.kaziki.nft.turbo.api.order.request.OrderCreateAndConfirmRequest;
import cn.kaziki.nft.turbo.api.order.request.OrderDiscardRequest;
import cn.kaziki.nft.turbo.api.order.response.OrderResponse;
import cn.kaziki.nft.turbo.api.order.service.OrderFacadeService;
import cn.kaziki.nft.turbo.api.order.service.OrderTransactionFacadeService;
import cn.kaziki.nft.turbo.api.user.constant.UserType;
import cn.kaziki.nft.turbo.base.response.SingleResponse;
import cn.kaziki.turbo.stream.consumer.AbstractStreamConsumer;
import cn.kaziki.turbo.stream.param.MessageBody;
import cn.hutool.core.lang.Assert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;


@Component
@Slf4j
public class NormalBuyMsgListener extends AbstractStreamConsumer {

    @Autowired
    private OrderFacadeService orderFacadeService;
    @Autowired
    private OrderTransactionFacadeService orderTransactionFacadeService;
    @Autowired
    private GoodsTransactionFacadeService goodsTransactionFacadeService;

    @Bean
    Consumer<Message<MessageBody>> normalBuyPreCancel() {
        return msg -> {
            OrderCreateAndConfirmRequest orderCreateAndConfirmRequest = getMessage(msg, OrderCreateAndConfirmRequest.class);
            SingleResponse<TradeOrderVO> response = orderFacadeService.getTradeOrder(orderCreateAndConfirmRequest.getOrderId());

            //如果订单已经创建成功，则直接返回。不再需要做废单处理了。
            if (response.getSuccess() && response.getData() != null && response.getData().getOrderState() == TradeOrderState.CONFIRM) {
                return;
            }

            doCancel(orderCreateAndConfirmRequest);
        };
    }

    @Bean
    Consumer<Message<MessageBody>> normalBuyCancel() {
        return msg -> {
            OrderCreateAndConfirmRequest orderCreateAndConfirmRequest = getMessage(msg, OrderCreateAndConfirmRequest.class);
            doCancel(orderCreateAndConfirmRequest);
        };
    }

    private void doCancel(OrderCreateAndConfirmRequest orderCreateAndConfirmRequest) {
        GoodsSaleRequest goodsSaleRequest = new GoodsSaleRequest(orderCreateAndConfirmRequest);
        // cancel库存
        boolean result = goodsTransactionFacadeService.cancelDecreaseInventory(goodsSaleRequest).getSuccess();
        Assert.isTrue(result, "order cancel failed");
        OrderDiscardRequest orderDiscardRequest = new OrderDiscardRequest();
        orderDiscardRequest.setOperatorType(UserType.PLATFORM);
        orderDiscardRequest.setOperator(UserType.PLATFORM.name());
        BeanUtils.copyProperties(orderCreateAndConfirmRequest, orderDiscardRequest);
        // 废弃订单
        OrderResponse orderResponse = orderTransactionFacadeService.cancelOrder(orderDiscardRequest,"normalBuy");
        Assert.isTrue(orderResponse.getSuccess(), orderResponse.getResponseCode());
    }
}
