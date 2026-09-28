package cn.kaziki.nft.turbo.pay.domain.entity;

import cn.kaziki.nft.turbo.api.pay.constant.PayOrderState;
import cn.kaziki.nft.turbo.pay.domain.event.PaySuccessEvent;
import cn.kaziki.nft.turbo.pay.domain.event.RefundSuccessEvent;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.Assert.*;

/**
 * PayOrder 实体单元测试 — 状态机流转 + 判断方法
 */
public class PayOrderTest {

    // ==================== paying — TO_PAY → PAYING ====================

    @Test
    public void testPaying_FromToPay_StateBecomesPaying() {
        PayOrder order = buildToPayOrder();
        order.paying("http://pay.example.com/qrcode");

        assertEquals(PayOrderState.PAYING, order.getOrderState());
        assertEquals("http://pay.example.com/qrcode", order.getPayUrl());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPaying_FromPaying_ThrowsException() {
        PayOrder order = buildToPayOrder();
        order.paying("url1");
        // 重复 paying 应抛异常
        order.paying("url2");
    }

    // ==================== paySuccess — PAYING → PAID ====================

    @Test
    public void testPaySuccess_FromPaying_StateBecomesPaid() {
        PayOrder order = buildToPayOrder();
        order.paying("url");

        PaySuccessEvent event = buildPaySuccessEvent();
        order.paySuccess(event);

        assertEquals(PayOrderState.PAID, order.getOrderState());
        assertEquals(event.getChannelStreamId(), order.getChannelStreamId());
        assertEquals(event.getPaidAmount(), order.getPaidAmount());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPaySuccess_FromToPay_ThrowsException() {
        // TO_PAY 状态直接 paySuccess 不合法
        PayOrder order = buildToPayOrder();
        order.paySuccess(buildPaySuccessEvent());
    }

    // ==================== payExpired — PAYING → EXPIRED ====================

    @Test
    public void testPayExpired_FromPaying_StateBecomesExpired() {
        PayOrder order = buildToPayOrder();
        order.paying("url");
        order.payExpired();

        assertEquals(PayOrderState.EXPIRED, order.getOrderState());
        assertNotNull(order.getPayExpireTime());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPayExpired_FromPaid_ThrowsException() {
        PayOrder order = buildToPayOrder();
        order.paying("url");
        order.paySuccess(buildPaySuccessEvent());
        // PAID 状态不可 expired
        order.payExpired();
    }

    // ==================== payFailed — PAYING → FAILED ====================

    @Test
    public void testPayFailed_FromPaying_StateBecomesFailed() {
        PayOrder order = buildToPayOrder();
        order.paying("url");
        order.payFailed();

        assertEquals(PayOrderState.FAILED, order.getOrderState());
        assertNotNull(order.getPayFailedTime());
    }

    // ==================== refundSuccess — PAID → REFUNDED ====================

    @Test
    public void testRefundSuccess_FromPaid_StateBecomesRefunded() {
        PayOrder order = buildToPayOrder();
        order.paying("url");
        order.paySuccess(buildPaySuccessEvent());

        RefundSuccessEvent refundEvent = buildRefundSuccessEvent();
        order.refundSuccess(refundEvent);

        assertEquals(PayOrderState.REFUNDED, order.getOrderState());
        assertEquals(refundEvent.getChannelStreamId(), order.getRefundChannelStreamId());
        assertEquals(refundEvent.getRefundedAmount(), order.getRefundedAmount());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRefundSuccess_FromPaying_ThrowsException() {
        PayOrder order = buildToPayOrder();
        order.paying("url");
        // PAYING 不能直接退款
        order.refundSuccess(buildRefundSuccessEvent());
    }

    // ==================== isPaid 判断 ====================

    @Test
    public void testIsPaid_StateIsPaid_ReturnsTrue() {
        PayOrder order = buildToPayOrder();
        order.paying("url");
        order.paySuccess(buildPaySuccessEvent());

        assertTrue(order.isPaid());
    }

    @Test
    public void testIsPaid_StateIsRefunded_ReturnsTrue() {
        PayOrder order = buildToPayOrder();
        order.paying("url");
        order.paySuccess(buildPaySuccessEvent());
        order.refundSuccess(buildRefundSuccessEvent());

        // REFUNDED 也是 paid 的一种（曾经支付过）
        assertTrue(order.isPaid());
    }

    @Test
    public void testIsPaid_StateIsToPay_ReturnsFalse() {
        PayOrder order = buildToPayOrder();
        assertFalse(order.isPaid());
    }

    @Test
    public void testIsPaid_StateIsPaying_ReturnsFalse() {
        PayOrder order = buildToPayOrder();
        order.paying("url");
        assertFalse(order.isPaid());
    }

    // ==================== isPayFailed 判断 ====================

    @Test
    public void testIsPayFailed_StateIsFailed_ReturnsTrue() {
        PayOrder order = buildToPayOrder();
        order.paying("url");
        order.payFailed();

        assertTrue(order.isPayFailed());
    }

    @Test
    public void testIsPayFailed_StateIsPaid_ReturnsFalse() {
        PayOrder order = buildToPayOrder();
        order.paying("url");
        order.paySuccess(buildPaySuccessEvent());

        assertFalse(order.isPayFailed());
    }

    // ==================== 辅助方法 ====================

    private PayOrder buildToPayOrder() {
        PayOrder order = new PayOrder();
        order.setPayOrderId("pay-001");
        order.setPayerId("user-001");
        order.setPayeeId("0");
        order.setBizNo("order-001");
        order.setOrderAmount(new BigDecimal("29.90"));
        order.setPaidAmount(BigDecimal.ZERO);
        order.setOrderState(PayOrderState.TO_PAY);
        return order;
    }

    private PaySuccessEvent buildPaySuccessEvent() {
        PaySuccessEvent event = new PaySuccessEvent();
        event.setChannelStreamId("wx-txn-12345");
        event.setPaidAmount(new BigDecimal("29.90"));
        event.setPaySucceedTime(new Date());
        return event;
    }

    private RefundSuccessEvent buildRefundSuccessEvent() {
        RefundSuccessEvent event = new RefundSuccessEvent();
        event.setChannelStreamId("wx-refund-67890");
        event.setRefundedAmount(new BigDecimal("29.90"));
        return event;
    }
}
