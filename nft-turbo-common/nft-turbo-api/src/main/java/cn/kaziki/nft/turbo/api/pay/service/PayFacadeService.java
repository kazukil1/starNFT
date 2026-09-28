package cn.kaziki.nft.turbo.api.pay.service;

import cn.kaziki.nft.turbo.api.pay.model.PayOrderVO;
import cn.kaziki.nft.turbo.api.pay.request.PayCreateRequest;
import cn.kaziki.nft.turbo.api.pay.request.PayQueryRequest;
import cn.kaziki.nft.turbo.api.pay.response.PayCreateResponse;
import cn.kaziki.nft.turbo.base.response.MultiResponse;
import cn.kaziki.nft.turbo.base.response.SingleResponse;

/**
 * 支付 rpc服务
 */
public interface PayFacadeService {

    /**
     * 生成支付链接
     *
     * @param payCreateRequest
     * @return
     */
    public PayCreateResponse generatePayUrl(PayCreateRequest payCreateRequest);

    /**
     * 查询支付订单
     *
     * @param payQueryRequest
     * @return
     */
    public MultiResponse<PayOrderVO> queryPayOrders(PayQueryRequest payQueryRequest);

    /**
     * 查询支付订单
     *
     * @param payOrderId
     * @return
     */
    public SingleResponse<PayOrderVO> queryPayOrder(String payOrderId);

    /**
     * 查询支付订单
     *
     * @param payOrderId
     * @param payerId
     * @return
     */
    public SingleResponse<PayOrderVO> queryPayOrder(String payOrderId, String payerId);


}
