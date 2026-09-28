package pay;

import cn.kaziki.nft.turbo.api.pay.constant.PayChannel;
import cn.kaziki.nft.turbo.api.pay.constant.PayOrderState;
import cn.kaziki.nft.turbo.pay.domain.entity.PayCheckMismatchDetail;
import cn.kaziki.nft.turbo.pay.domain.entity.PayOrder;
import cn.kaziki.nft.turbo.pay.domain.entity.WechatTransaction;
import cn.kaziki.nft.turbo.pay.domain.service.PayCheckMismatchDetailService;
import cn.kaziki.nft.turbo.pay.domain.service.PayOrderService;
import cn.kaziki.nft.turbo.pay.domain.service.WechatTransactionService;
import cn.kaziki.nft.turbo.pay.job.PayDetailCheckJob;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class PayDetailCheckJobTest extends PayBaseTest {

    @Autowired
    PayDetailCheckJob payDetailCheckJob;

    @MockBean
    private PayOrderService payOrderService;

    @MockBean
    private WechatTransactionService wechatTransactionService;

    @Autowired
    private PayCheckMismatchDetailService payCheckMismatchDetailService;

    @Test
    public void testDoExecute_Success() {
        PayOrder payOrder = new PayOrder();
        payOrder.setPayOrderId(UUID.randomUUID().toString());
        payOrder.setOrderState(PayOrderState.PAID);
        payOrder.setPaidAmount(BigDecimal.TEN);
        payOrder.setPayChannel(PayChannel.MOCK);

        WechatTransaction wechatTransaction = new WechatTransaction();
        wechatTransaction.setAmount(payOrder.getPaidAmount());
        wechatTransaction.setStatus("SUCCESS");
        when(wechatTransactionService.queryByMchOrderNo(anyString())).thenReturn(wechatTransaction);
        payDetailCheckJob.doExecute(payOrder);
    }

    @Test
    public void testDoExecute_StatusCheckFailed() {
        PayOrder payOrder = new PayOrder();
        payOrder.setPayOrderId(UUID.randomUUID().toString());
        payOrder.setOrderState(PayOrderState.PAID);
        payOrder.setPaidAmount(BigDecimal.TEN);
        payOrder.setPayChannel(PayChannel.MOCK);
        payOrder.setPaySucceedTime(new Date());

        WechatTransaction wechatTransaction = new WechatTransaction();
        wechatTransaction.setAmount(payOrder.getPaidAmount());
        wechatTransaction.setStatus("INIT");
        wechatTransaction.setMchOrderNo(payOrder.getPayOrderId());

        when(wechatTransactionService.queryByMchOrderNo(anyString())).thenReturn(wechatTransaction);

        payDetailCheckJob.doExecute(payOrder);

        PayCheckMismatchDetail existPayCheckMismatchDetail = payCheckMismatchDetailService.getOne(new QueryWrapper<PayCheckMismatchDetail>().eq("pay_order_id", payOrder.getPayOrderId()));
        Assert.assertNotNull(existPayCheckMismatchDetail);

        Assert.assertEquals(existPayCheckMismatchDetail.getPayOrderId(), payOrder.getPayOrderId());
        Assert.assertEquals(existPayCheckMismatchDetail.getPayOrderState(), payOrder.getOrderState().name());
        Assert.assertEquals(0, existPayCheckMismatchDetail.getPayOrderTime().compareTo(payOrder.getPaySucceedTime()));
        Assert.assertEquals(0, existPayCheckMismatchDetail.getPayOrderAmount().compareTo(payOrder.getPaidAmount()));


        Assert.assertEquals(existPayCheckMismatchDetail.getChannelStreamId(), wechatTransaction.getMchOrderNo());
        Assert.assertEquals("INIT", existPayCheckMismatchDetail.getChannelStreamState());
        Assert.assertEquals(0, wechatTransaction.getAmount().compareTo(existPayCheckMismatchDetail.getChannelStreamAmount()));
        Assert.assertEquals("INIT", existPayCheckMismatchDetail.getStatus());
        Assert.assertEquals("CHANNEL_STREAM_STATUS_NOT_SUCCESS", existPayCheckMismatchDetail.getMismatchType().name());
    }

    @Test
    public void testDoExecute_ChannelNotExist() {
        PayOrder payOrder = new PayOrder();
        payOrder.setPayOrderId(UUID.randomUUID().toString());
        payOrder.setOrderState(PayOrderState.PAID);
        payOrder.setPaidAmount(BigDecimal.TEN);
        payOrder.setPayChannel(PayChannel.MOCK);
        payOrder.setPaySucceedTime(new Date());

        when(wechatTransactionService.queryByMchOrderNo(anyString())).thenReturn(null);

        payDetailCheckJob.doExecute(payOrder);

        PayCheckMismatchDetail existPayCheckMismatchDetail = payCheckMismatchDetailService.getOne(new QueryWrapper<PayCheckMismatchDetail>().eq("pay_order_id", payOrder.getPayOrderId()));
        Assert.assertNotNull(existPayCheckMismatchDetail);

        Assert.assertEquals(existPayCheckMismatchDetail.getPayOrderId(), payOrder.getPayOrderId());
        Assert.assertEquals(existPayCheckMismatchDetail.getPayOrderState(), payOrder.getOrderState().name());
        Assert.assertEquals(0, existPayCheckMismatchDetail.getPayOrderTime().compareTo(payOrder.getPaySucceedTime()));
        Assert.assertEquals(0, existPayCheckMismatchDetail.getPayOrderAmount().compareTo(payOrder.getPaidAmount()));

        Assert.assertNull(existPayCheckMismatchDetail.getChannelStreamId());
        Assert.assertNull(existPayCheckMismatchDetail.getChannelStreamState());
        Assert.assertNull(existPayCheckMismatchDetail.getChannelStreamAmount());
        Assert.assertEquals("INIT", existPayCheckMismatchDetail.getStatus());
        Assert.assertEquals("CHANNEL_TRANSACTION_NOT_EXIST", existPayCheckMismatchDetail.getMismatchType().name());
    }

    @Test
    public void testDoExecute_AmountCheckFailed() {
        PayOrder payOrder = new PayOrder();
        payOrder.setPayOrderId(UUID.randomUUID().toString());
        payOrder.setOrderState(PayOrderState.PAID);
        payOrder.setPaidAmount(BigDecimal.TEN);
        payOrder.setPayChannel(PayChannel.MOCK);
        payOrder.setPaySucceedTime(new Date());

        WechatTransaction wechatTransaction = new WechatTransaction();
        wechatTransaction.setAmount(BigDecimal.ONE);
        wechatTransaction.setStatus("SUCCESS");
        wechatTransaction.setMchOrderNo(payOrder.getPayOrderId());

        when(wechatTransactionService.queryByMchOrderNo(anyString())).thenReturn(wechatTransaction);

        payDetailCheckJob.doExecute(payOrder);

        PayCheckMismatchDetail existPayCheckMismatchDetail = payCheckMismatchDetailService.getOne(new QueryWrapper<PayCheckMismatchDetail>().eq("pay_order_id", payOrder.getPayOrderId()));
        Assert.assertNotNull(existPayCheckMismatchDetail);

        Assert.assertEquals(existPayCheckMismatchDetail.getPayOrderId(), payOrder.getPayOrderId());
        Assert.assertEquals(existPayCheckMismatchDetail.getPayOrderState(), payOrder.getOrderState().name());
        Assert.assertEquals(0, existPayCheckMismatchDetail.getPayOrderTime().compareTo(payOrder.getPaySucceedTime()));
        Assert.assertEquals(0, existPayCheckMismatchDetail.getPayOrderAmount().compareTo(payOrder.getPaidAmount()));


        Assert.assertEquals(existPayCheckMismatchDetail.getChannelStreamId(), wechatTransaction.getMchOrderNo());
        Assert.assertEquals("SUCCESS", existPayCheckMismatchDetail.getChannelStreamState());
        Assert.assertEquals(0, wechatTransaction.getAmount().compareTo(existPayCheckMismatchDetail.getChannelStreamAmount()));
        Assert.assertEquals("INIT", existPayCheckMismatchDetail.getStatus());
        Assert.assertEquals("CHANNEL_STREAM_AMOUNT_NOT_EQUAL_PAY_ORDER_AMOUNT", existPayCheckMismatchDetail.getMismatchType().name());
    }
}
