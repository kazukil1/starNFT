package cn.kaziki.nft.turbo.order.domain.service;

import cn.kaziki.nft.turbo.api.order.constant.OrderErrorCode;
import cn.kaziki.nft.turbo.api.order.constant.TradeOrderEvent;
import cn.kaziki.nft.turbo.api.order.request.*;
import cn.yueyu.nft.turbo.api.order.request.*;
import cn.kaziki.nft.turbo.api.order.response.OrderResponse;
import cn.kaziki.nft.turbo.api.user.constant.UserType;
import cn.kaziki.nft.turbo.base.exception.BizException;
import cn.kaziki.nft.turbo.base.exception.RepoErrorCode;
import cn.kaziki.nft.turbo.base.utils.BeanValidator;
import cn.kaziki.nft.turbo.order.domain.entity.TradeOrder;
import cn.kaziki.nft.turbo.order.domain.entity.TradeOrderStream;
import cn.kaziki.nft.turbo.order.domain.exception.OrderException;
import cn.kaziki.nft.turbo.order.domain.listener.event.OrderCreateEvent;
import cn.kaziki.nft.turbo.order.infrastructure.mapper.OrderMapper;
import cn.kaziki.nft.turbo.order.infrastructure.mapper.OrderStreamMapper;
import cn.hutool.core.lang.Assert;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.shardingsphere.transaction.annotation.ShardingSphereTransactionType;
import org.apache.shardingsphere.transaction.core.TransactionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Consumer;
import java.util.function.Function;

import static cn.kaziki.nft.turbo.api.order.constant.OrderErrorCode.ORDER_NOT_EXIST;
import static cn.kaziki.nft.turbo.api.order.constant.OrderErrorCode.PERMISSION_DENIED;
import static cn.kaziki.nft.turbo.base.response.ResponseCode.SYSTEM_ERROR;
import static java.util.Objects.requireNonNull;

/**
 * 订单服务
 */
@Service
public class OrderManageService extends ServiceImpl<OrderMapper, TradeOrder> {

    private static final Logger logger = LoggerFactory.getLogger(OrderManageService.class);

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderStreamMapper orderStreamMapper;

    @Autowired
    protected TransactionTemplate transactionTemplate;

    @Autowired
    protected ApplicationContext applicationContext;

    // 创建订单 -tcc
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse createForTcc(OrderCreateRequest request) {
        // 1.前置校验-幂等查询
        TradeOrder existOrder = orderMapper.selectByIdentifier(request.getIdentifier(), request.getBuyerId());
        if (existOrder != null) {
            return new OrderResponse.OrderResponseBuilder().orderId(existOrder.getOrderId()).buildSuccess();
        }
        // 2.订单创建
        TradeOrder tradeOrder = TradeOrder.createOrder(request);
        boolean res = save(tradeOrder);
        Assert.isTrue(res, () -> new BizException(RepoErrorCode.INSERT_FAILED));
        // 3.订单流水创建
        TradeOrderStream orderStream = new TradeOrderStream(tradeOrder, request.getOrderEvent(), request.getIdentifier());
        res = orderStreamMapper.insert(orderStream) == 1;
        Assert.isTrue(res, () -> new BizException(RepoErrorCode.INSERT_FAILED));

        return new OrderResponse.OrderResponseBuilder().orderId(tradeOrder.getOrderId()).buildSuccess();
    }

    // 创建订单
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse create(OrderCreateRequest request) {
        // 1.前置校验-幂等查询:业务id++sequence++用户id
        TradeOrder existOrder = orderMapper.selectByIdentifier(request.getIdentifier(), request.getBuyerId());
        if (existOrder != null) {
            return new OrderResponse.OrderResponseBuilder().orderId(existOrder.getOrderId()).buildSuccess();
        }
        // 2.订单创建,状态:create,同步处理
        TradeOrder tradeOrder = TradeOrder.createOrder(request);
        // 3.保存订单
        boolean res = save(tradeOrder);
        Assert.isTrue(res, () -> new BizException(RepoErrorCode.INSERT_FAILED));
        // 4.订单流水创建
        TradeOrderStream orderStream = new TradeOrderStream(tradeOrder, request.getOrderEvent(), request.getIdentifier());
        res = orderStreamMapper.insert(orderStream) == 1;
        Assert.isTrue(res, () -> new BizException(RepoErrorCode.INSERT_FAILED));
        // 5.抛出事件 -- 推进订单状态:见OrderEventListener
        //事件机,监听OrderCreateEvent,推进订单状态到create
        applicationContext.publishEvent(new OrderCreateEvent(tradeOrder));
        return new OrderResponse.OrderResponseBuilder().orderId(tradeOrder.getOrderId()).buildSuccess();
    }

    // 确认订单
    public OrderResponse confirm(OrderConfirmRequest request) {
        return doExecute(request, tradeOrder -> tradeOrder.confirm(request));
    }

    // 创建并确认订单
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse createAndConfirm(OrderCreateAndConfirmRequest request) {
        // 1.流水校验
        TradeOrder existOrder = orderMapper.selectByIdentifier(request.getIdentifier(), request.getBuyerId());
        if (existOrder != null) {
            return new OrderResponse.OrderResponseBuilder().orderId(existOrder.getOrderId()).buildSuccess();
        }

        // 2.创建订单
        TradeOrder tradeOrder = TradeOrder.createOrder(request);
        OrderConfirmRequest confirmRequest = new OrderConfirmRequest();
        BeanUtils.copyProperties(request, confirmRequest);
        confirmRequest.setOrderId(tradeOrder.getOrderId());

        // 3.确认订单
        tradeOrder.confirm(confirmRequest);

        // 4.更新数据库
        boolean result = save(tradeOrder);
        Assert.isTrue(result, () -> new OrderException(OrderErrorCode.CREATE_ORDER_FAILED));

        // 5.添加订单流水
        TradeOrderStream orderStream = new TradeOrderStream(tradeOrder, request.getOrderEvent(), request.getIdentifier());
        result = orderStreamMapper.insert(orderStream) == 1;
        Assert.isTrue(result, () -> new OrderException(RepoErrorCode.INSERT_FAILED));

        return new OrderResponse.OrderResponseBuilder().orderId(tradeOrder.getOrderId()).buildSuccess();
    }

    // 废弃订单
    public OrderResponse discard(OrderDiscardRequest request) {
        return doExecute(request, tradeOrder -> tradeOrder.discard(request));
    }

    // 取消订单
    public OrderResponse cancel(OrderCancelRequest request) {
        return doExecute(request, tradeOrder -> tradeOrder.close(request));
    }

    // 订单超时
    public OrderResponse timeout(OrderTimeoutRequest request) {
        return doExecute(request, tradeOrder -> tradeOrder.close(request));
    }

    // 支付订单
    @Transactional(rollbackFor = Exception.class)
    @ShardingSphereTransactionType(TransactionType.BASE)
    public OrderResponse paySuccess(OrderPayRequest request) {
        return doExecuteWithOutTrans(request, tradeOrder -> tradeOrder.paySuccess(request));
    }

    // 订单完结
    public OrderResponse finish(OrderFinishRequest request) {
        return doExecute(request, tradeOrder -> tradeOrder.finish(request));
    }

    // 通用订单更新逻辑
    protected OrderResponse doExecute(BaseOrderUpdateRequest orderRequest, Consumer<TradeOrder> consumer) {
        OrderResponse response = new OrderResponse();
        return handle(orderRequest, response, "doExecute", request -> {
            // 1.查询当前订单
            TradeOrder existOrder = orderMapper.selectByOrderId(request.getOrderId());
            if (existOrder == null) {
                throw new OrderException(ORDER_NOT_EXIST);
            }
            // 2.鉴权
            if (!hasPermission(existOrder, orderRequest.getOrderEvent(), orderRequest.getOperator(), orderRequest.getOperatorType())) {
                throw new OrderException(PERMISSION_DENIED);
            }
            // 3.流水校验
            TradeOrderStream existStream = orderStreamMapper.selectByIdentifier(orderRequest.getIdentifier(), orderRequest.getOrderEvent().name(), orderRequest.getOrderId());
            if (existStream != null) {
                return new OrderResponse.OrderResponseBuilder().orderId(existStream.getOrderId()).streamId(existStream.getId().toString()).buildDuplicated();
            }

            // 4.核心逻辑执行,转换订单状态
            consumer.accept(existOrder);

            // 5.开启事务
            return transactionTemplate.execute(transactionStatus -> {

                boolean result = orderMapper.updateByOrderId(existOrder) == 1;
                Assert.isTrue(result, () -> new OrderException(OrderErrorCode.UPDATE_ORDER_FAILED));

                TradeOrderStream orderStream = new TradeOrderStream(existOrder, orderRequest.getOrderEvent(), orderRequest.getIdentifier());
                // 新增订单流水
                result = orderStreamMapper.insert(orderStream) == 1;
                Assert.isTrue(result, () -> new BizException(RepoErrorCode.INSERT_FAILED));

                return new OrderResponse.OrderResponseBuilder().orderId(orderStream.getOrderId()).streamId(String.valueOf(orderStream.getId())).buildSuccess();
            });
        });
    }

    // 通用订单更新逻辑(不带事务，需要调用方自己保证事务)
    protected OrderResponse doExecuteWithOutTrans(BaseOrderUpdateRequest orderRequest, Consumer<TradeOrder> consumer) {
        OrderResponse response = new OrderResponse();
        return handle(orderRequest, response, "doExecute", request -> {
            // 1.查询订单
            TradeOrder existOrder = orderMapper.selectByOrderId(request.getOrderId());
            if (existOrder == null) {
                throw new OrderException(ORDER_NOT_EXIST);
            }
            // 2.校验权限
            if (!hasPermission(existOrder, orderRequest.getOrderEvent(), orderRequest.getOperator(), orderRequest.getOperatorType())) {
                throw new OrderException(PERMISSION_DENIED);
            }
            // 3.查看流水避免重复操作
            TradeOrderStream existStream = orderStreamMapper.selectByIdentifier(orderRequest.getIdentifier(), orderRequest.getOrderEvent().name(), orderRequest.getOrderId());
            if (existStream != null) {
                return new OrderResponse.OrderResponseBuilder().orderId(existStream.getOrderId()).streamId(existStream.getId().toString()).buildDuplicated();
            }

            // 4.核心逻辑执行
            consumer.accept(existOrder);
            boolean result = orderMapper.updateByOrderId(existOrder) == 1;
            Assert.isTrue(result, () -> new OrderException(OrderErrorCode.UPDATE_ORDER_FAILED));
            // 5.新增流水
            TradeOrderStream orderStream = new TradeOrderStream(existOrder, orderRequest.getOrderEvent(), orderRequest.getIdentifier());
            result = orderStreamMapper.insert(orderStream) == 1;
            Assert.isTrue(result, () -> new BizException(RepoErrorCode.INSERT_FAILED));

            return new OrderResponse.OrderResponseBuilder().orderId(orderStream.getOrderId()).streamId(String.valueOf(orderStream.getId())).buildSuccess();

        });
    }

    private boolean hasPermission(TradeOrder existOrder, TradeOrderEvent orderEvent, String operator, UserType operatorType) {
        switch (orderEvent) {
            case PAY:
            case CANCEL:
            case CREATE_AND_CONFIRM:
                return existOrder.getBuyerId().equals(operator);
            case TIME_OUT:
            case CONFIRM:
            case FINISH:
            case DISCARD:
                return operatorType == UserType.PLATFORM;
            default:
                throw new UnsupportedOperationException("unsupport order event : " + orderEvent);
        }
    }

    public static <T, R extends OrderResponse> OrderResponse handle(T request, R response, String method, Function<T, R> function) {
        logger.info("before execute method={}, request={}", method, JSON.toJSONString(request));
        try {
            requireNonNull(request);
            BeanValidator.validateObject(request);
            response = function.apply(request);
        } catch (OrderException e) {
            logger.error(e.toString(), e);
            response.setSuccess(false);
            response.setResponseCode(e.getErrorCode().getCode());
            response.setResponseMessage(e.getErrorCode().getMessage());
            logger.error("failed execute method={}, exception={}", method, JSON.toJSONString(e));
        } catch (Exception e) {
            response.setSuccess(false);
            response.setResponseCode(SYSTEM_ERROR.name());
            response.setResponseMessage(e.getMessage());
            logger.error("failed execute method={}, exception={}", method, JSON.toJSONString(e));
        } finally {
            logger.info("after execute method={}, result={}", method, JSON.toJSONString(response));
        }
        return response;
    }



}
