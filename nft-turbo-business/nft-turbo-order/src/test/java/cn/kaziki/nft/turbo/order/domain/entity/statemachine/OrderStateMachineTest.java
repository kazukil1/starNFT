package cn.kaziki.nft.turbo.order.domain.entity.statemachine;

import cn.kaziki.nft.turbo.api.order.constant.TradeOrderEvent;
import cn.kaziki.nft.turbo.api.order.constant.TradeOrderState;
import cn.kaziki.nft.turbo.base.exception.BizException;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * OrderStateMachine 单元测试 — 全部合法/非法状态流转
 */
public class OrderStateMachineTest {

    private final OrderStateMachine machine = OrderStateMachine.INSTANCE;

    // ==================== 合法流转 ====================

    @Test
    public void testTransition_CreateToConfirm_ReturnsConfirm() {
        TradeOrderState result = machine.transition(TradeOrderState.CREATE, TradeOrderEvent.CONFIRM);
        assertEquals(TradeOrderState.CONFIRM, result);
    }

    @Test
    public void testTransition_ConfirmToPaid_ReturnsPaid() {
        TradeOrderState result = machine.transition(TradeOrderState.CONFIRM, TradeOrderEvent.PAY);
        assertEquals(TradeOrderState.PAID, result);
    }

    @Test
    public void testTransition_CreateToPaid_ReturnsPaid() {
        // 库存预扣减成功但未真正扣减时也能支付
        TradeOrderState result = machine.transition(TradeOrderState.CREATE, TradeOrderEvent.PAY);
        assertEquals(TradeOrderState.PAID, result);
    }

    @Test
    public void testTransition_CreateToClosedByCancel_ReturnsClosed() {
        TradeOrderState result = machine.transition(TradeOrderState.CREATE, TradeOrderEvent.CANCEL);
        assertEquals(TradeOrderState.CLOSED, result);
    }

    @Test
    public void testTransition_CreateToClosedByTimeout_ReturnsClosed() {
        TradeOrderState result = machine.transition(TradeOrderState.CREATE, TradeOrderEvent.TIME_OUT);
        assertEquals(TradeOrderState.CLOSED, result);
    }

    @Test
    public void testTransition_CreateToDiscard_ReturnsDiscard() {
        TradeOrderState result = machine.transition(TradeOrderState.CREATE, TradeOrderEvent.DISCARD);
        assertEquals(TradeOrderState.DISCARD, result);
    }

    @Test
    public void testTransition_ConfirmToDiscard_ReturnsDiscard() {
        TradeOrderState result = machine.transition(TradeOrderState.CONFIRM, TradeOrderEvent.DISCARD);
        assertEquals(TradeOrderState.DISCARD, result);
    }

    @Test
    public void testTransition_ConfirmToClosedByCancel_ReturnsClosed() {
        TradeOrderState result = machine.transition(TradeOrderState.CONFIRM, TradeOrderEvent.CANCEL);
        assertEquals(TradeOrderState.CLOSED, result);
    }

    @Test
    public void testTransition_ConfirmToClosedByTimeout_ReturnsClosed() {
        TradeOrderState result = machine.transition(TradeOrderState.CONFIRM, TradeOrderEvent.TIME_OUT);
        assertEquals(TradeOrderState.CLOSED, result);
    }

    @Test
    public void testTransition_PaidToConfirm_Idempotent_ReturnsPaid() {
        // 已支付后再确认，状态不变（幂等）
        TradeOrderState result = machine.transition(TradeOrderState.PAID, TradeOrderEvent.CONFIRM);
        assertEquals(TradeOrderState.PAID, result);
    }

    @Test
    public void testTransition_PaidToFinish_ReturnsFinish() {
        TradeOrderState result = machine.transition(TradeOrderState.PAID, TradeOrderEvent.FINISH);
        assertEquals(TradeOrderState.FINISH, result);
    }

    // ==================== 非法流转 ====================

    @Test(expected = BizException.class)
    public void testTransition_PaidToCancel_ThrowsException() {
        machine.transition(TradeOrderState.PAID, TradeOrderEvent.CANCEL);
    }

    @Test(expected = BizException.class)
    public void testTransition_ClosedToConfirm_ThrowsException() {
        machine.transition(TradeOrderState.CLOSED, TradeOrderEvent.CONFIRM);
    }

    @Test(expected = BizException.class)
    public void testTransition_ClosedToPay_ThrowsException() {
        machine.transition(TradeOrderState.CLOSED, TradeOrderEvent.PAY);
    }

    @Test(expected = BizException.class)
    public void testTransition_FinishToCancel_ThrowsException() {
        machine.transition(TradeOrderState.FINISH, TradeOrderEvent.CANCEL);
    }

    @Test(expected = BizException.class)
    public void testTransition_FinishToPay_ThrowsException() {
        machine.transition(TradeOrderState.FINISH, TradeOrderEvent.PAY);
    }

    @Test(expected = BizException.class)
    public void testTransition_DiscardToConfirm_ThrowsException() {
        machine.transition(TradeOrderState.DISCARD, TradeOrderEvent.CONFIRM);
    }

    @Test(expected = BizException.class)
    public void testTransition_DiscardToPay_ThrowsException() {
        machine.transition(TradeOrderState.DISCARD, TradeOrderEvent.PAY);
    }

    // ==================== 边界 ====================

    @Test
    public void testTransition_SameInstance_SingletonCorrect() {
        // 验证单例一致
        OrderStateMachine another = OrderStateMachine.INSTANCE;
        assertSame(machine, another);
    }
}
