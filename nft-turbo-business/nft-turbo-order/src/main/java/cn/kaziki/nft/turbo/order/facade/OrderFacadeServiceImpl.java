package cn.kaziki.nft.turbo.order.facade;

import cn.kaziki.nft.turbo.api.goods.request.GoodsSaleRequest;
import cn.kaziki.nft.turbo.api.goods.response.GoodsSaleResponse;
import cn.kaziki.nft.turbo.api.goods.service.GoodsFacadeService;
import cn.kaziki.nft.turbo.api.inventory.request.InventoryRequest;
import cn.kaziki.nft.turbo.api.inventory.service.InventoryFacadeService;
import cn.kaziki.nft.turbo.api.order.request.*;
import cn.kaziki.nft.turbo.api.order.service.OrderFacadeService;
import cn.kaziki.nft.turbo.api.order.constant.OrderErrorCode;
import cn.kaziki.nft.turbo.api.order.model.TradeOrderVO;
import cn.yueyu.nft.turbo.api.order.request.*;
import cn.kaziki.nft.turbo.api.order.response.OrderResponse;
import cn.kaziki.nft.turbo.api.user.constant.UserType;
import cn.kaziki.nft.turbo.api.user.request.UserQueryRequest;
import cn.kaziki.nft.turbo.api.user.response.UserQueryResponse;
import cn.kaziki.nft.turbo.api.user.response.data.UserInfo;
import cn.kaziki.nft.turbo.api.user.service.UserFacadeService;
import cn.kaziki.nft.turbo.base.response.BaseResponse;
import cn.kaziki.nft.turbo.base.response.PageResponse;
import cn.kaziki.nft.turbo.base.response.SingleResponse;
import cn.kaziki.nft.turbo.lock.DistributeLock;
import cn.kaziki.nft.turbo.order.domain.entity.TradeOrder;
import cn.kaziki.nft.turbo.order.domain.entity.convertor.TradeOrderConvertor;
import cn.kaziki.nft.turbo.order.domain.exception.OrderException;
import cn.kaziki.nft.turbo.order.domain.service.OrderManageService;
import cn.kaziki.nft.turbo.order.domain.service.OrderReadService;
import cn.kaziki.nft.turbo.order.validator.OrderCreateValidator;
import cn.kaziki.nft.turbo.rpc.facade.Facade;
import cn.kaziki.turbo.stream.producer.StreamProducer;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import groovyjarjarantlr4.v4.runtime.misc.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static cn.kaziki.nft.turbo.api.order.constant.OrderErrorCode.ORDER_CREATE_VALID_FAILED;

/**
 * 订单 rpc服务实现类
 */
@DubboService(version = "1.0.0")
@Slf4j
public class OrderFacadeServiceImpl implements OrderFacadeService {

    @Autowired
    private OrderReadService orderReadService;

    @Autowired
    private OrderManageService orderManageService;

    @Autowired
    private UserFacadeService userFacadeService;

    @Autowired
    private OrderCreateValidator orderCreateValidator;

    @Autowired
    private InventoryFacadeService inventoryFacadeService;

    @Autowired
    private GoodsFacadeService goodsFacadeService;

    @Autowired
    private StreamProducer streamProducer;

    @Autowired
    private OrderCreateValidator orderValidator;

    // 创建订单
    @Override
    //幂等校验:判断是否有库存扣减流水:key为goodsId+identifier,value为change
    //分布式锁:key:identifier
    @DistributeLock(keyExpression = "#request.identifier", scene = "ORDER_CREATE")
    @Facade
    public OrderResponse create(OrderCreateRequest request) {
        try {
            //前置校验,校验顺序见:OrderClientConfiguration
            //1.判断是不是平台用户和正常买家
            //2.判断商品是否存在且可售
            //3.是否预约商品
            orderCreateValidator.validate(request);
        } catch (OrderException e) {
            return new OrderResponse.OrderResponseBuilder().buildFail(ORDER_CREATE_VALID_FAILED.getCode(), e.getErrorCode().getMessage());
        }
        //扣减缓存库存
        InventoryRequest inventoryRequest = new InventoryRequest(request);
        SingleResponse<Boolean> decreaseResponse = inventoryFacadeService.decrease(inventoryRequest);

        //订单创建
        if (decreaseResponse.getSuccess()) {
            return orderManageService.create(request);
        }
        throw new OrderException(OrderErrorCode.INVENTORY_DECREASE_FAILED);
    }

    // 创建订单并确认
    @Override
    @DistributeLock(keyExpression = "#request.identifier", scene = "ORDER_CREATE")
    @Facade
    public OrderResponse createAndConfirm(OrderCreateAndConfirmRequest request) {
        // 1.责任链校验
        try {
            orderValidator.validate(request);
        } catch (OrderException e) {
            //  校验失败，ORDER_CREATE_VALID_FAILED
            return new OrderResponse.OrderResponseBuilder().orderId(request.getOrderId()).buildFail(ORDER_CREATE_VALID_FAILED.getCode(), e.getErrorCode().getMessage());
        }

        // 2.扣减数据库库存
        if (request.isSyncDecreaseInventory()) {
            GoodsSaleRequest goodsSaleRequest = new GoodsSaleRequest(request);
            GoodsSaleResponse response = goodsFacadeService.saleWithoutHint(goodsSaleRequest);
            if (!response.getSuccess()) {
                // 此处处理sentinel限流拒绝
                return new OrderResponse.OrderResponseBuilder().buildFail(response.getResponseMessage(), response.getResponseCode());
            }
        }

        // 3.创建订单并确认
        OrderResponse orderResponse = orderManageService.createAndConfirm(request);

        return orderResponse;
    }

    // 取消订单
    @Override
    @Facade
    public OrderResponse cancel(OrderCancelRequest request) {
        //使用事务消息进行关单
        return sendTransactionMsgForClose(request);
    }

    // 订单超时关单
    @Override
    @Facade
    public OrderResponse timeout(OrderTimeoutRequest request) {
        //使用事务消息进行关单
        return sendTransactionMsgForClose(request);
    }

    @NotNull
    private OrderResponse sendTransactionMsgForClose(BaseOrderUpdateRequest request) {
        //因为RocketMQ 的事务消息中，如果本地事务发生了异常，这里返回也会是个 true，所以就需要做一下反查进行二次判断，才能知道关单操作是否成功
        //消息监听：TradeOrderListener
        streamProducer.send("orderClose-out-0", null, JSON.toJSONString(request), "CLOSE_TYPE", request.getOrderEvent().name());
        TradeOrder tradeOrder = orderReadService.getOrder(request.getOrderId());
        OrderResponse orderResponse = new OrderResponse();
        if (tradeOrder.isClosed()) {
            orderResponse.setSuccess(true);
        } else {
            orderResponse.setSuccess(false);
        }
        return orderResponse;
    }


    // 确认订单
    @Override
    @Facade
    public OrderResponse confirm(OrderConfirmRequest request) {
        GoodsSaleRequest goodsSaleRequest = new GoodsSaleRequest();
        goodsSaleRequest.setUserId(request.getBuyerId());
        goodsSaleRequest.setGoodsId(Long.valueOf(request.getGoodsId()));
        goodsSaleRequest.setGoodsType(request.getGoodsType().name());
        goodsSaleRequest.setIdentifier(request.getOrderId());
        goodsSaleRequest.setQuantity(request.getItemCount());
        // 1.更新藏品可售库存
        BaseResponse response = goodsFacadeService.sale(goodsSaleRequest);
        if (response.getSuccess()) {
            // 2.更新订单状态
            return orderManageService.confirm(request);
        }
        return new OrderResponse.OrderResponseBuilder().orderId(request.getOrderId()).buildFail(
                response.getResponseCode(), response.getResponseMessage()
        );
    }

    // 支付成功
    @Override
    @Facade
    public OrderResponse paySuccess(OrderPayRequest request) {
        OrderResponse response = orderManageService.paySuccess(request);
        if (!response.getSuccess()) {
            TradeOrder existOrder = orderReadService.getOrder(request.getOrderId());
            if (existOrder != null && existOrder.isClosed()) {
                return new OrderResponse.OrderResponseBuilder().orderId(existOrder.getOrderId()).buildFail(OrderErrorCode.ORDER_ALREADY_CLOSED.getCode(), OrderErrorCode.ORDER_ALREADY_CLOSED.getMessage());
            }
            if (existOrder != null && existOrder.isPaid()) {
                if (existOrder.getPayStreamId().equals(request.getPayStreamId()) && existOrder.getPayChannel() == request.getPayChannel()) {
                    return new OrderResponse.OrderResponseBuilder().orderId(existOrder.getOrderId()).buildSuccess();
                } else {
                    return new OrderResponse.OrderResponseBuilder().orderId(existOrder.getOrderId()).buildFail(OrderErrorCode.ORDER_ALREADY_PAID.getCode(), OrderErrorCode.ORDER_ALREADY_PAID.getMessage());
                }
            }
        }
        return response;
    }

    @Override
    public SingleResponse<TradeOrderVO> getTradeOrder(String orderId) {
        return SingleResponse.of(TradeOrderConvertor.INSTANCE.mapToVo(orderReadService.getOrder(orderId)));
    }

    @Override
    @Facade
    public SingleResponse<TradeOrderVO> getTradeOrder(String orderId, String userId) {
        return SingleResponse.of(TradeOrderConvertor.INSTANCE.mapToVo(orderReadService.getOrder(orderId, userId)));
    }

    @Override
    @Facade
    public PageResponse<TradeOrderVO> pageQuery(OrderPageQueryRequest request) {
        Page<TradeOrder> page = orderReadService.pageQueryByState(request.getBuyerId(), request.getState(), request.getCurrentPage(), request.getPageSize());
        List<TradeOrderVO> tradeOrderVOS = TradeOrderConvertor.INSTANCE.mapToVo(page.getRecords());
        tradeOrderVOS.forEach(tradeOrderVO -> tradeOrderVO.setSellerName(getSellerName(tradeOrderVO)));
        return PageResponse.of(tradeOrderVOS, (int) page.getTotal(), request.getPageSize(), request.getCurrentPage());
    }

    // 获取卖家姓名（平台、普通用户）
    private String getSellerName(TradeOrderVO tradeOrderVO) {
        // 平台
        if (tradeOrderVO.getSellerType() == UserType.PLATFORM) {
            return "平台";
        }
        // 普通用户
        UserQueryRequest userQueryRequest = new UserQueryRequest(Long.valueOf(tradeOrderVO.getSellerId()));
        UserQueryResponse<UserInfo> userQueryResponse = userFacadeService.query(userQueryRequest);
        if (userQueryResponse.getSuccess()) {
            return userQueryResponse.getData().getNickName();
        }
        return "-";
    }
}
