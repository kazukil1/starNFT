package cn.kaziki.nft.turbo.order.listener;

import cn.kaziki.nft.turbo.api.goods.constant.GoodsEvent;
import cn.kaziki.nft.turbo.api.goods.model.GoodsStreamVO;
import cn.kaziki.nft.turbo.api.goods.request.GoodsSaleRequest;
import cn.kaziki.nft.turbo.api.goods.service.GoodsFacadeService;
import cn.kaziki.nft.turbo.api.inventory.request.InventoryRequest;
import cn.kaziki.nft.turbo.api.inventory.service.InventoryFacadeService;
import cn.kaziki.nft.turbo.api.order.constant.OrderErrorCode;
import cn.kaziki.nft.turbo.api.order.request.OrderCreateAndConfirmRequest;
import cn.kaziki.nft.turbo.api.order.request.OrderCreateRequest;
import cn.kaziki.nft.turbo.api.order.response.OrderResponse;
import cn.kaziki.nft.turbo.api.order.service.OrderFacadeService;
import cn.kaziki.nft.turbo.api.user.constant.UserType;
import cn.kaziki.nft.turbo.base.response.SingleResponse;
import cn.kaziki.nft.turbo.order.OrderException;
import cn.kaziki.nft.turbo.order.domain.entity.TradeOrder;
import cn.kaziki.nft.turbo.order.domain.service.OrderReadService;
import cn.kaziki.turbo.stream.consumer.AbstractStreamConsumer;
import cn.kaziki.turbo.stream.param.MessageBody;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.function.Consumer;

/**
 * <p>
 * 单条消费MQ的newBuy消息，在rocketmq.broker.check=fasle （stream.yml） 的时候会生效
 * 这个Bean和NewBuyBatchMsgListener只启动一个。
 * 本Bean对RocketMQ的Brocker部署不强依赖，即不部署也不到会导致应用无法启动，但是消息会无法发送和消费
 */
@Component
@Slf4j
@ConditionalOnProperty(value = "rocketmq.broker.check", havingValue = "false", matchIfMissing = true)
public class NewBuyMsgListener extends AbstractStreamConsumer {

    @Autowired
    private OrderFacadeService orderFacadeService;

    @Autowired
    private OrderReadService orderReadService;

    @Autowired
    private InventoryFacadeService inventoryFacadeService;

    @Autowired
    private GoodsFacadeService goodsFacadeService;

    @Bean
    Consumer<Message<MessageBody>> newBuy() {
        return msg -> {
            OrderCreateRequest orderCreateRequest = getMessage(msg, OrderCreateRequest.class);
            doNewBuyExecute(orderCreateRequest);
        };
    }

    public void doNewBuyExecute(OrderCreateRequest orderCreateRequest) {
        // 1.创建订单并确认订单
        OrderCreateAndConfirmRequest orderCreateAndConfirmRequest = new OrderCreateAndConfirmRequest();
        BeanUtils.copyProperties(orderCreateRequest, orderCreateAndConfirmRequest);
        orderCreateAndConfirmRequest.setOperator(UserType.PLATFORM.name());
        orderCreateAndConfirmRequest.setOperatorType(UserType.PLATFORM);
        orderCreateAndConfirmRequest.setOperateTime(new Date());
        orderCreateAndConfirmRequest.setSyncDecreaseInventory(true);
        OrderResponse orderResponse = orderFacadeService.createAndConfirm(orderCreateAndConfirmRequest);

        // 2.下单失败，回滚库存
        if (!orderResponse.getSuccess()) {
            String orderId = orderCreateRequest.getOrderId();
            TradeOrder tradeOrder = orderReadService.getOrder(orderId);
            // 3.再查一次确认订单不存在（避免并发）
            if (tradeOrder == null) {
                // 3.1 查 TRY_SALE 流水，判断 saleWithoutHint 是否已在 goods 模块事务中提交
                GoodsStreamVO goodsStream = goodsFacadeService.getGoodsInventoryStream(
                        orderCreateRequest.getGoodsId(),
                        orderCreateRequest.getGoodsType(),
                        GoodsEvent.TRY_SALE,
                        orderId
                );
                if (goodsStream != null) {
                    // saleWithoutHint 已提交，DB 库存已扣 → 回滚 DB 库存
                    GoodsSaleRequest goodsSaleRequest = new GoodsSaleRequest();
                    goodsSaleRequest.setGoodsId(Long.valueOf(orderCreateRequest.getGoodsId()));
                    goodsSaleRequest.setGoodsType(orderCreateRequest.getGoodsType().name());
                    goodsSaleRequest.setIdentifier(orderId);
                    goodsSaleRequest.setQuantity(orderCreateRequest.getItemCount());
                    goodsSaleRequest.setExtendInfo("创建订单失败，回滚库存");
                    if (!goodsFacadeService.cancelSale(goodsSaleRequest).getSuccess()) {
                        log.error("cancelSale failed, orderId={}", orderId);
                        throw new OrderException(OrderErrorCode.INVENTORY_INCREASE_FAILED);
                    }
                }

                // 3.2 回滚 Redis 库存
                InventoryRequest inventoryRequest = new InventoryRequest();
                inventoryRequest.setGoodsId(orderCreateRequest.getGoodsId());
                inventoryRequest.setInventory(orderCreateRequest.getItemCount());
                inventoryRequest.setIdentifier(orderId);
                inventoryRequest.setGoodsType(orderCreateRequest.getGoodsType());
                SingleResponse<Boolean> increaseResponse = inventoryFacadeService.increase(inventoryRequest);
                if (!increaseResponse.getSuccess()) {
                    log.error("increase inventory failed, orderCreateRequest:{} , increaseResponse:{}",
                            JSON.toJSONString(orderCreateRequest), JSON.toJSONString(increaseResponse));
                    throw new OrderException(OrderErrorCode.INVENTORY_INCREASE_FAILED);
                }
                // 清理 Redis 记录
                inventoryFacadeService.removeInventoryDecreaseLog(inventoryRequest);
                inventoryFacadeService.removeInventoryIncreaseLog(inventoryRequest);
                log.info("reids inventory rollback success, orderId={}", orderId);
            } else {
                log.warn("order create failed in response but order exists in DB, orderId={}", orderId);
            }
        }
    }
}
