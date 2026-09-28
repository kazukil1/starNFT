package cn.kaziki.nft.turbo.order.domain.entity;

import cn.kaziki.nft.turbo.api.goods.constant.GoodsType;
import cn.kaziki.nft.turbo.api.order.constant.TradeOrderEvent;
import cn.kaziki.nft.turbo.api.order.constant.TradeOrderState;
import cn.kaziki.nft.turbo.api.order.request.*;
import cn.yueyu.nft.turbo.api.order.request.*;
import cn.kaziki.nft.turbo.api.pay.constant.PayChannel;
import cn.kaziki.nft.turbo.api.user.constant.UserType;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.Assert.*;

/**
 * TradeOrder 实体单元测试 — 工厂方法 + 状态转换 + 判断方法
 */
public class TradeOrderTest {

    // ==================== 工厂方法 createOrder ====================

    @Test
    public void testCreateOrder_ValidRequest_StateIsCreate() {
        OrderCreateRequest request = buildCreateRequest();
        TradeOrder order = TradeOrder.createOrder(request);

        assertEquals(TradeOrderState.CREATE, order.getOrderState());
        assertEquals(BigDecimal.ZERO, order.getPaidAmount());
        assertEquals("order-001", order.getOrderId());
        assertEquals("user-001", order.getBuyerId());
        assertNotNull(order.getReverseBuyerId());
    }

    // ==================== confirm — CREATE → CONFIRM ====================

    @Test
    public void testConfirm_FromCreate_StateBecomesConfirm() {
        TradeOrder order = TradeOrder.createOrder(buildCreateRequest());
        OrderConfirmRequest req = buildConfirmRequest(order.getOrderId());

        order.confirm(req);
        assertEquals(TradeOrderState.CONFIRM, order.getOrderState());
    }

    // ==================== pay — → PAID ====================

    @Test
    public void testPay_FromCreate_StateBecomesPaid() {
        TradeOrder order = TradeOrder.createOrder(buildCreateRequest());
        OrderPayRequest req = buildPayRequest(order.getOrderId());

        order.pay(req);
        assertEquals(TradeOrderState.PAID, order.getOrderState());
        assertEquals("pay-stream-001", order.getPayStreamId());
        assertEquals(PayChannel.MOCK, order.getPayChannel());
    }

    // PAID + PAY 在状态机中无对应转移（不是幂等，是不合法），
    // 幂等场景应由 OrderFacadeServiceImpl.paySuccess() 在业务层处理

    // ==================== close — CREATE/CONFIRM → CLOSED ====================

    @Test
    public void testClose_FromCreateByCancel_StateBecomesClosed() {
        TradeOrder order = TradeOrder.createOrder(buildCreateRequest());
        OrderCancelRequest req = buildCancelRequest(order.getOrderId());

        order.close(req);
        assertEquals(TradeOrderState.CLOSED, order.getOrderState());
        assertEquals(TradeOrderEvent.CANCEL.name(), order.getCloseType());
    }

    @Test
    public void testClose_ByTimeout_CloseTypeIsTimeout() {
        TradeOrder order = TradeOrder.createOrder(buildCreateRequest());
        OrderTimeoutRequest req = buildTimeoutRequest(order.getOrderId());

        order.close(req);
        assertEquals(TradeOrderState.CLOSED, order.getOrderState());
        assertEquals(TradeOrderEvent.TIME_OUT.name(), order.getCloseType());
    }

    // ==================== finish — PAID → FINISH ====================

    @Test
    public void testFinish_FromPaid_StateBecomesFinish() {
        TradeOrder order = TradeOrder.createOrder(buildCreateRequest());
        order.pay(buildPayRequest(order.getOrderId()));

        OrderFinishRequest req = buildFinishRequest(order.getOrderId());
        order.finish(req);
        assertEquals(TradeOrderState.FINISH, order.getOrderState());
    }

    // ==================== paySuccess — 同 pay ====================

    @Test
    public void testPaySuccess_FromConfirm_SetsPayInfo() {
        TradeOrder order = TradeOrder.createOrder(buildCreateRequest());
        order.confirm(buildConfirmRequest(order.getOrderId()));

        OrderPayRequest req = buildPayRequest(order.getOrderId());
        order.paySuccess(req);
        assertEquals(TradeOrderState.PAID, order.getOrderState());
        assertEquals(new BigDecimal("29.90"), order.getPaidAmount());
    }

    // ==================== discard — → DISCARD ====================

    @Test
    public void testDiscard_FromCreate_StateBecomesDiscard() {
        TradeOrder order = TradeOrder.createOrder(buildCreateRequest());
        OrderDiscardRequest req = buildDiscardRequest(order.getOrderId());

        order.discard(req);
        assertEquals(TradeOrderState.DISCARD, order.getOrderState());
    }

    // ==================== 判断方法 ====================

    @Test
    public void testIsPaid_StateIsPaid_ReturnsTrue() {
        TradeOrder order = TradeOrder.createOrder(buildCreateRequest());
        order.pay(buildPayRequest(order.getOrderId()));
        assertTrue(order.isPaid());
    }

    @Test
    public void testIsPaid_StateIsCreate_ReturnsFalse() {
        TradeOrder order = TradeOrder.createOrder(buildCreateRequest());
        assertFalse(order.isPaid());
    }

    @Test
    public void testIsPaid_StateIsFinish_ReturnsTrue() {
        TradeOrder order = TradeOrder.createOrder(buildCreateRequest());
        order.pay(buildPayRequest(order.getOrderId()));
        order.finish(buildFinishRequest(order.getOrderId()));
        assertTrue(order.isPaid());
    }

    @Test
    public void testIsClosed_StateIsClosed_ReturnsTrue() {
        TradeOrder order = TradeOrder.createOrder(buildCreateRequest());
        order.close(buildCancelRequest(order.getOrderId()));
        assertTrue(order.isClosed());
    }

    @Test
    public void testIsClosed_StateIsCreate_ReturnsFalse() {
        TradeOrder order = TradeOrder.createOrder(buildCreateRequest());
        assertFalse(order.isClosed());
    }

    @Test
    public void testIsConfirmed_StateIsConfirm_ReturnsTrue() {
        TradeOrder order = TradeOrder.createOrder(buildCreateRequest());
        order.confirm(buildConfirmRequest(order.getOrderId()));
        assertTrue(order.isConfirmed());
    }

    @Test
    public void testIsConfirmed_StateIsCreate_ReturnsFalse() {
        TradeOrder order = TradeOrder.createOrder(buildCreateRequest());
        assertFalse(order.isConfirmed());
    }

    @Test
    public void testGetPayExpireTime_AfterCreation() {
        TradeOrder order = TradeOrder.createOrder(buildCreateRequest());
        // gmtCreate 通常由 MyBatis-Plus 自动填充，单测需手动设置
        order.setGmtCreate(new Date());
        Date expireTime = order.getPayExpireTime();
        assertNotNull(expireTime);
        assertTrue(expireTime.after(order.getGmtCreate()));
    }

    // ==================== 非法状态流转 ====================

    @Test(expected = RuntimeException.class)
    public void testDiscard_ThenPay_ThrowsException() {
        TradeOrder order = TradeOrder.createOrder(buildCreateRequest());
        order.discard(buildDiscardRequest(order.getOrderId()));
        // DISCARD → PAY 非法
        order.pay(buildPayRequest(order.getOrderId()));
    }

    // ==================== 辅助方法 ====================

    private OrderCreateRequest buildCreateRequest() {
        OrderCreateRequest req = new OrderCreateRequest();
        req.setOrderId("order-001");
        req.setBuyerId("user-001");
        req.setBuyerType(UserType.CUSTOMER);
        req.setSellerId("0");
        req.setSellerType(UserType.PLATFORM);
        req.setGoodsId("100");
        req.setGoodsType(GoodsType.COLLECTION);
        req.setGoodsName("测试藏品");
        req.setItemCount(1L);
        req.setItemPrice(new BigDecimal("29.90"));
        req.setOrderAmount(new BigDecimal("29.90"));
        req.setIdentifier("idempotent-order-001");
        return req;
    }

    private OrderConfirmRequest buildConfirmRequest(String orderId) {
        OrderConfirmRequest req = new OrderConfirmRequest();
        req.setOrderId(orderId);
        req.setOperateTime(new Date());
        req.setOperator("system");
        req.setOperatorType(UserType.PLATFORM);
        req.setBuyerId("user-001");
        req.setGoodsId("100");
        req.setGoodsType(GoodsType.COLLECTION);
        req.setItemCount(1L);
        return req;
    }

    private OrderPayRequest buildPayRequest(String orderId) {
        OrderPayRequest req = new OrderPayRequest();
        req.setOrderId(orderId);
        req.setOperateTime(new Date());
        req.setOperator("user-001");
        req.setOperatorType(UserType.CUSTOMER);
        req.setPayStreamId("pay-stream-001");
        req.setPayChannel(PayChannel.MOCK);
        req.setAmount(new BigDecimal("29.90"));
        return req;
    }

    private OrderCancelRequest buildCancelRequest(String orderId) {
        OrderCancelRequest req = new OrderCancelRequest();
        req.setOrderId(orderId);
        req.setOperateTime(new Date());
        req.setOperator("user-001");
        req.setOperatorType(UserType.CUSTOMER);
        return req;
    }

    private OrderTimeoutRequest buildTimeoutRequest(String orderId) {
        OrderTimeoutRequest req = new OrderTimeoutRequest();
        req.setOrderId(orderId);
        req.setOperateTime(new Date());
        req.setOperator("system");
        req.setOperatorType(UserType.PLATFORM);
        return req;
    }

    private OrderFinishRequest buildFinishRequest(String orderId) {
        OrderFinishRequest req = new OrderFinishRequest();
        req.setOrderId(orderId);
        req.setOperateTime(new Date());
        req.setOperator("system");
        req.setOperatorType(UserType.PLATFORM);
        return req;
    }

    private OrderDiscardRequest buildDiscardRequest(String orderId) {
        OrderDiscardRequest req = new OrderDiscardRequest();
        req.setOrderId(orderId);
        req.setOperateTime(new Date());
        req.setOperator("system");
        req.setOperatorType(UserType.PLATFORM);
        return req;
    }
}
