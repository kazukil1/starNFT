package cn.kaziki.nft.turbo.api.order.service;


import cn.kaziki.nft.turbo.api.order.request.OrderConfirmRequest;
import cn.kaziki.nft.turbo.api.order.request.OrderCreateRequest;
import cn.kaziki.nft.turbo.api.order.request.OrderDiscardRequest;
import cn.kaziki.nft.turbo.api.order.response.OrderResponse;

/**
 * 订单服务 tcc事务1
 */
public interface OrderTransactionFacadeService {

    // 预创建订单
    public OrderResponse tryOrder(OrderCreateRequest orderCreateRequest, String businessScene);

    // 确认订单
    public OrderResponse confirmOrder(OrderConfirmRequest orderConfirmRequest, String businessScene);

    // 撤销订单
    public OrderResponse cancelOrder(OrderDiscardRequest orderDiscardRequest, String businessScene);
}
