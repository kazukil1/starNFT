package cn.kaziki.nft.turbo.trade.application;

import cn.kaziki.nft.turbo.api.goods.request.GoodsSaleRequest;
import cn.kaziki.nft.turbo.api.goods.service.GoodsTransactionFacadeService;
import cn.kaziki.nft.turbo.api.inventory.service.InventoryTransactionFacadeService;
import cn.kaziki.nft.turbo.api.inventory.request.InventoryRequest;
import cn.kaziki.nft.turbo.api.inventory.service.InventoryFacadeService;
import cn.kaziki.nft.turbo.api.order.service.OrderFacadeService;
import cn.kaziki.nft.turbo.api.order.service.OrderTransactionFacadeService;
import cn.kaziki.nft.turbo.api.order.request.OrderConfirmRequest;
import cn.kaziki.nft.turbo.api.order.request.OrderCreateAndConfirmRequest;
import cn.kaziki.nft.turbo.api.order.response.OrderResponse;
import cn.kaziki.nft.turbo.base.response.SingleResponse;
import cn.kaziki.turbo.stream.producer.StreamProducer;
import cn.hutool.core.lang.Assert;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static cn.kaziki.nft.turbo.api.order.constant.OrderErrorCode.INVENTORY_DECREASE_FAILED;
import static cn.kaziki.nft.turbo.trade.exception.TradeErrorCode.*;
import static cn.kaziki.turbo.stream.producer.StreamProducer.DELAY_LEVEL_1_M;
import static cn.kaziki.turbo.stream.producer.StreamProducer.DELAY_LEVEL_30_S;

@Service
@Slf4j
public class TradeApplicationService {

    private static final int MAX_RETRY_TIMES = 1;

    @Autowired
    private OrderTransactionFacadeService orderTransactionFacadeService;
    @Autowired
    private GoodsTransactionFacadeService goodsTransactionFacadeService;
    @Autowired
    private InventoryTransactionFacadeService inventoryTransactionFacadeService;
    @Autowired
    private StreamProducer streamProducer;
    @Autowired
    private InventoryFacadeService inventoryFacadeService;
    @Autowired
    private OrderFacadeService orderFacadeService;

    /**
     * 普通交易，基于TCC实现分布式一致性
     * Try -> Confirm ：Try成功，执行Confirm
     * Try --> Cancel ： Try失败，执行Cancel
     * Try -> Confirm --> Cancel ：Try成功，Confirm失败，执行Cancel
     */
    public OrderResponse normalBuy(OrderCreateAndConfirmRequest orderCreateRequest) {
        boolean isTrySuccess = true;
        // 1.Try
        try {
            // 冻结库存
            GoodsSaleRequest goodsSaleRequest = new GoodsSaleRequest(orderCreateRequest);
            boolean result = goodsTransactionFacadeService.tryDecreaseInventory(goodsSaleRequest).getSuccess();
            Assert.isTrue(result, "decrease inventory failed");

            // 预创建订单
            result = orderTransactionFacadeService.tryOrder(orderCreateRequest, "normalBuy").getSuccess();
            Assert.isTrue(result, "order create failed");
        } catch (Exception e) {
            isTrySuccess = false;
            log.error("normalBuy try failed, ", e);
        }

        // 2.Try失败，发【废单消息】，异步进行逆向补偿
        if (!isTrySuccess) {
            //消息监听： NormalBuyMsgListener
            streamProducer.send("normalBuyCancel-out-0", orderCreateRequest.getGoodsType().name(), JSON.toJSONString(orderCreateRequest));
            return new OrderResponse.OrderResponseBuilder().buildFail(NORMAL_BUY_TCC_CANCEL_FAILED.getCode(), NORMAL_BUY_TCC_CANCEL_FAILED.getMessage());
        }

        // 3.Confirm
        boolean isConfirmSuccess = false;
        int retryConfirmCount = 0;

        // 最大努力执行，失败最多尝试2次.（Dubbo也会有重试机制，在服务突然不可用、超时等情况下会重试2次）
        while (!isConfirmSuccess && retryConfirmCount < MAX_RETRY_TIMES) {
            try {
                GoodsSaleRequest goodsSaleRequest = new GoodsSaleRequest(orderCreateRequest);
                // 解冻并扣减库存
                isConfirmSuccess = goodsTransactionFacadeService.confirmDecreaseInventory(goodsSaleRequest).getSuccess();
                Assert.isTrue(isConfirmSuccess, "confirmDecreaseInventory failed");

                OrderConfirmRequest orderConfirmRequest = new OrderConfirmRequest();
                BeanUtils.copyProperties(orderCreateRequest, orderConfirmRequest);
                // 确认订单
                isConfirmSuccess = orderTransactionFacadeService.confirmOrder(orderConfirmRequest, "normalBuy").getSuccess();
                Assert.isTrue(isConfirmSuccess, "confirmOrder failed");
            } catch (Exception e) {
                retryConfirmCount++;
                isConfirmSuccess = false;
                log.error("normalBuy confirm failed, ", e);
            }
        }

        // 4.Confirm失败，发【疑似废单消息】进行延迟检查
        if (!isConfirmSuccess) {
            //消息监听： NormalBuyMsgListener
            streamProducer.send("normalBuyPreCancel-out-0", orderCreateRequest.getGoodsType().name(), JSON.toJSONString(orderCreateRequest), DELAY_LEVEL_1_M);
            return new OrderResponse.OrderResponseBuilder().buildFail(NORMAL_BUY_TCC_CONFIRM_FAILED.getCode(), NORMAL_BUY_TCC_CONFIRM_FAILED.getMessage());
        }

        return new OrderResponse.OrderResponseBuilder().orderId(orderCreateRequest.getOrderId()).buildSuccess();
    }


    /**
     * 秒杀第三套方案，基于TCC实现分布式一致性
     * <p>
     * Try -> Confirm ：Try成功，执行Confirm
     * Try --> Cancel ： Try失败，执行Cancel
     * Try -> Confirm --> Cancel ：Try成功，Confirm失败，执行Cancel
     *
     * @param orderCreateRequest
     * @returnc
     * @Deprecated 因为这个TCC的方案，会有很多次数据库的操作（6次），所以会导致数据库的IO多，CPU高，不建议使用，除非花钱去提升数据库的硬件配置，否则的话建议用newBuyPlus
     */
    @Deprecated
    public OrderResponse newBuyPlusByTcc(OrderCreateAndConfirmRequest orderCreateRequest) {

        boolean isTrySuccess = true;

        //Try
        try {
            InventoryRequest inventoryRequest = new InventoryRequest(orderCreateRequest);
            // 冻结库存
            boolean result = inventoryTransactionFacadeService.tryDecrease(inventoryRequest);
            Assert.isTrue(result, "decrease inventory failed");

            // 创建订单
            result = orderTransactionFacadeService.tryOrder(orderCreateRequest, "newBuyPlus").getSuccess();
            Assert.isTrue(result, "order create failed");
        } catch (Exception e) {
            isTrySuccess = false;
            log.error("newBuyPlus try failed, ", e);
        }

        //Try失败，发【废单消息】，异步进行逆向补偿
        if (!isTrySuccess) {
            //消息监听： NewBuyPlusMsgListener
            streamProducer.send("newBuyPlusCancel-out-0", orderCreateRequest.getGoodsType().name(), JSON.toJSONString(orderCreateRequest));
            return new OrderResponse.OrderResponseBuilder().buildFail(NORMAL_BUY_TCC_CANCEL_FAILED.getCode(), NORMAL_BUY_TCC_CANCEL_FAILED.getMessage());
        }

        //Confirm
        boolean isConfirmSuccess = false;
        int retryConfirmCount = 0;

        //最大努力执行，失败最多尝试2次.（Dubbo也会有重试机制，在服务突然不可用、超时等情况下会重试2次）
        while (!isConfirmSuccess && retryConfirmCount < MAX_RETRY_TIMES) {
            try {
                // 解冻并扣减库存
                isConfirmSuccess = inventoryTransactionFacadeService.confirmDecrease(new InventoryRequest(orderCreateRequest));
                Assert.isTrue(isConfirmSuccess, "confirmDecrease failed");
                OrderConfirmRequest orderConfirmRequest = new OrderConfirmRequest();
                BeanUtils.copyProperties(orderCreateRequest, orderConfirmRequest);
                // 确认订单
                isConfirmSuccess = orderTransactionFacadeService.confirmOrder(orderConfirmRequest, "newBuyPlus").getSuccess();
                Assert.isTrue(isConfirmSuccess, "confirmOrder failed");
            } catch (Exception e) {
                retryConfirmCount++;
                isConfirmSuccess = false;
                log.error("newBuyPlus confirm failed, ", e);
            }
        }

        //Confirm失败，发【疑似废单消息】进行延迟检查
        if (!isConfirmSuccess) {
            //消息监听： NewBuyPlusMsgListener
            streamProducer.send("newBuyPlusPreCancel-out-0", orderCreateRequest.getGoodsType().name(), JSON.toJSONString(orderCreateRequest), DELAY_LEVEL_1_M);
            return new OrderResponse.OrderResponseBuilder().buildFail(NORMAL_BUY_TCC_CONFIRM_FAILED.getCode(), NORMAL_BUY_TCC_CONFIRM_FAILED.getMessage());
        }

        return new OrderResponse.OrderResponseBuilder().orderId(orderCreateRequest.getOrderId()).buildSuccess();
    }

    /**
     * 秒杀第三套方案，不基于TCC
     *
     * @param orderCreateRequest
     * @returnc
     */
    public OrderResponse newBuyPlus(OrderCreateAndConfirmRequest orderCreateRequest) {

        //1、扣减Redis库存
        InventoryRequest inventoryRequest = new InventoryRequest(orderCreateRequest);
        try {
            SingleResponse<Boolean> response = inventoryFacadeService.decrease(inventoryRequest);
            Assert.isTrue(response.getSuccess() && response.getData(), "decrease inventory failed");
        } catch (Exception e) {
            SingleResponse<String> decreaseLogResp = inventoryFacadeService.getInventoryDecreaseLog(inventoryRequest);
            if (!decreaseLogResp.getSuccess() || decreaseLogResp.getData() == null) {
                return new OrderResponse.OrderResponseBuilder().buildFail(INVENTORY_DECREASE_FAILED.getCode(), INVENTORY_DECREASE_FAILED.getMessage());
            }
        }

        //2、创建订单
        try {
            orderCreateRequest.setSyncDecreaseInventory(false);
            OrderResponse orderResponse = orderFacadeService.createAndConfirm(orderCreateRequest);
            Assert.isTrue(orderResponse.getSuccess(), "createAndConfirm failed");
        } catch (Exception e) {
            streamProducer.send("newBuyPlusPreCancel-out-0", orderCreateRequest.getGoodsType().name(), JSON.toJSONString(orderCreateRequest), DELAY_LEVEL_30_S);
            return new OrderResponse.OrderResponseBuilder().buildFail(ORDER_CREATE_FAILED.getCode(), ORDER_CREATE_FAILED.getMessage());
        }

        return new OrderResponse.OrderResponseBuilder().orderId(orderCreateRequest.getOrderId()).buildSuccess();
    }
}
