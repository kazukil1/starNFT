package cn.kaziki.nft.turbo.pay.facade.service;

import cn.kaziki.nft.turbo.api.pay.constant.PayErrorCode;
import cn.kaziki.nft.turbo.api.pay.constant.PayOrderState;
import cn.kaziki.nft.turbo.api.pay.model.PayOrderVO;
import cn.kaziki.nft.turbo.api.pay.request.PayCreateRequest;
import cn.kaziki.nft.turbo.api.pay.request.PayQueryByBizNo;
import cn.kaziki.nft.turbo.api.pay.request.PayQueryCondition;
import cn.kaziki.nft.turbo.api.pay.request.PayQueryRequest;
import cn.kaziki.nft.turbo.api.pay.response.PayCreateResponse;
import cn.kaziki.nft.turbo.api.pay.service.PayFacadeService;
import cn.kaziki.nft.turbo.base.exception.BizException;
import cn.kaziki.nft.turbo.base.exception.RepoErrorCode;
import cn.kaziki.nft.turbo.base.response.MultiResponse;
import cn.kaziki.nft.turbo.base.response.SingleResponse;
import cn.kaziki.nft.turbo.base.utils.MoneyUtils;
import cn.kaziki.nft.turbo.lock.DistributeLock;
import cn.kaziki.nft.turbo.pay.domain.entity.PayOrder;
import cn.kaziki.nft.turbo.pay.domain.entity.convertor.PayOrderConvertor;
import cn.kaziki.nft.turbo.pay.domain.service.PayOrderService;
import cn.kaziki.nft.turbo.pay.infrastructure.channel.common.request.PayChannelRequest;
import cn.kaziki.nft.turbo.pay.infrastructure.channel.common.response.PayChannelResponse;
import cn.kaziki.nft.turbo.pay.infrastructure.channel.common.service.PayChannelServiceFactory;
import cn.kaziki.nft.turbo.rpc.facade.Facade;
import cn.hutool.core.lang.Assert;
import org.apache.dubbo.config.annotation.DubboService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 支付 rpc服务
 */
@DubboService(version = "1.0.0")
public class PayFacadeServiceImpl implements PayFacadeService {

    private static final Logger logger = LoggerFactory.getLogger(PayFacadeServiceImpl.class);

    @Autowired
    private PayOrderService payOrderService;

    @Autowired
    private PayChannelServiceFactory payChannelServiceFactory;

    // 生成支付地址
    @Facade
    @DistributeLock(keyExpression = "#payCreateRequest.bizNo", scene = "GENERATE_PAY_URL")
    @Override
    public PayCreateResponse generatePayUrl(PayCreateRequest payCreateRequest) {
        PayCreateResponse response = new PayCreateResponse();
        // 1.创建支付单
        PayOrder payOrder = payOrderService.create(payCreateRequest);
        //  判断订单是否已经在支付中
        if (payOrder.getOrderState() == PayOrderState.PAYING) {
            response.setPayOrderId(payOrder.getPayOrderId());
            response.setPayUrl(payOrder.getPayUrl());
            response.setSuccess(true);
            return response;
        }
        //  判断订单是否支付成功
        if (payOrder.isPaid()) {
            response.setSuccess(false);
            response.setResponseCode(PayErrorCode.ORDER_IS_ALREADY_PAID.getCode());
            response.setResponseMessage(PayErrorCode.ORDER_IS_ALREADY_PAID.getMessage());
            return response;
        }
        // 2.获取支付链接
        PayChannelResponse payChannelResponse = doPay(payCreateRequest, payOrder);
        if (payChannelResponse.getSuccess()) {
            boolean updateResult = payOrderService.paying(payOrder.getPayOrderId(), payChannelResponse.getPayUrl());
            Assert.isTrue(updateResult, () -> new BizException(RepoErrorCode.UPDATE_FAILED));
            response.setSuccess(true);
            response.setPayOrderId(payOrder.getPayOrderId());
            response.setPayUrl(payChannelResponse.getPayUrl());
        } else {
            response.setSuccess(false);
            response.setResponseCode(payChannelResponse.getResponseCode());
            response.setResponseMessage(payChannelResponse.getResponseMessage());
        }
        return response;
    }

    // 查询支付单
    @Override
    @Facade
    public MultiResponse<PayOrderVO> queryPayOrders(PayQueryRequest payQueryRequest) {

        PayQueryCondition payQueryCondition = payQueryRequest.getPayQueryCondition();

        if (payQueryCondition instanceof PayQueryByBizNo payQueryByBizNo) {
            List<PayOrder> payOrders = payOrderService.queryByBizNo(payQueryByBizNo.getBizNo(), payQueryByBizNo.getBizType(), payQueryRequest.getPayerId(), payQueryRequest.getPayOrderState());
            var payQueryResponse = new MultiResponse<PayOrderVO>();
            payQueryResponse.setSuccess(true);
            payQueryResponse.setDatas(PayOrderConvertor.INSTANCE.mapToVo(payOrders));
            return payQueryResponse;
        }

        throw new UnsupportedOperationException("unsupported payQueryCondition : " + payQueryCondition);
    }

    @Override
    @Facade
    public SingleResponse<PayOrderVO> queryPayOrder(String payOrderId) {
        return SingleResponse.of(PayOrderConvertor.INSTANCE.mapToVo(payOrderService.queryByOrderId(payOrderId)));
    }

    @Override
    public SingleResponse<PayOrderVO> queryPayOrder(String payOrderId, String payerId) {
        return SingleResponse.of(PayOrderConvertor.INSTANCE.mapToVo(payOrderService.queryByOrderIdAndPayer(payOrderId, payerId)));
    }

    // 获取支付链接
    private PayChannelResponse doPay(PayCreateRequest payCreateRequest, PayOrder payOrder) {
        PayChannelRequest payChannelRequest = new PayChannelRequest();
        payChannelRequest.setAmount(MoneyUtils.yuanToCent(payCreateRequest.getOrderAmount()));
        payChannelRequest.setDescription(payCreateRequest.getMemo());
        payChannelRequest.setOrderId(payOrder.getPayOrderId());
        payChannelRequest.setAttach(payCreateRequest.getBizNo());
        payChannelRequest.setExpireTime(payOrder.getPayExpireTime());
        PayChannelResponse payChannelResponse = payChannelServiceFactory.get(payCreateRequest.getPayChannel()).pay(payChannelRequest);
        return payChannelResponse;
    }
}
