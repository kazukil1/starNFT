package cn.kaziki.nft.turbo.api.order.service;

import cn.kaziki.nft.turbo.api.order.model.TradeOrderVO;
import cn.kaziki.nft.turbo.api.order.request.*;
import cn.yueyu.nft.turbo.api.order.request.*;
import cn.kaziki.nft.turbo.api.order.response.OrderResponse;
import cn.kaziki.nft.turbo.base.response.PageResponse;
import cn.kaziki.nft.turbo.base.response.SingleResponse;

/**
 * 订单 rpc服务
 */
public interface OrderFacadeService {

    // 创建订单
    public OrderResponse create(OrderCreateRequest request);

    // 取消订单
    public OrderResponse cancel(OrderCancelRequest request);

    // 订单超时
    public OrderResponse timeout(OrderTimeoutRequest request);

    // 订单确认
    public OrderResponse confirm(OrderConfirmRequest request);

    // 订单创建并确认
    OrderResponse createAndConfirm(OrderCreateAndConfirmRequest orderCreateAndConfirmRequest);

    // 订单支付成功
    public OrderResponse paySuccess(OrderPayRequest request);

    // 订单详情
    public SingleResponse<TradeOrderVO> getTradeOrder(String orderId);

    // 订单详情
    public SingleResponse<TradeOrderVO> getTradeOrder(String orderId, String userId);

    // 订单分页查询
    public PageResponse<TradeOrderVO> pageQuery(OrderPageQueryRequest request);

}
