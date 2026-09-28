package cn.kaziki.nft.turbo.trade.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.kaziki.nft.turbo.api.check.request.InventoryCheckRequest;
import cn.kaziki.nft.turbo.api.check.response.InventoryCheckResponse;
import cn.kaziki.nft.turbo.api.check.service.InventoryCheckFacadeService;
import cn.kaziki.nft.turbo.api.common.constant.BizOrderType;
import cn.kaziki.nft.turbo.api.common.constant.BusinessCode;
import cn.kaziki.nft.turbo.api.goods.constant.GoodsEvent;
import cn.kaziki.nft.turbo.api.goods.constant.GoodsType;
import cn.kaziki.nft.turbo.api.goods.model.BaseGoodsVO;
import cn.kaziki.nft.turbo.api.goods.request.GoodsBookRequest;
import cn.kaziki.nft.turbo.api.goods.response.GoodsBookResponse;
import cn.kaziki.nft.turbo.api.goods.service.GoodsFacadeService;
import cn.kaziki.nft.turbo.api.inventory.request.InventoryRequest;
import cn.kaziki.nft.turbo.api.inventory.service.InventoryFacadeService;
import cn.kaziki.nft.turbo.api.order.constant.TradeOrderState;
import cn.kaziki.nft.turbo.api.order.model.TradeOrderVO;
import cn.kaziki.nft.turbo.api.order.request.OrderCancelRequest;
import cn.kaziki.nft.turbo.api.order.request.OrderCreateAndConfirmRequest;
import cn.kaziki.nft.turbo.api.order.request.OrderCreateRequest;
import cn.kaziki.nft.turbo.api.order.request.OrderTimeoutRequest;
import cn.kaziki.nft.turbo.api.order.response.OrderResponse;
import cn.kaziki.nft.turbo.api.order.service.OrderFacadeService;
import cn.kaziki.nft.turbo.api.pay.model.PayOrderVO;
import cn.kaziki.nft.turbo.api.pay.request.PayCreateRequest;
import cn.kaziki.nft.turbo.api.pay.response.PayCreateResponse;
import cn.kaziki.nft.turbo.api.pay.service.PayFacadeService;
import cn.kaziki.nft.turbo.api.user.constant.UserType;
import cn.kaziki.nft.turbo.base.response.SingleResponse;
import cn.kaziki.nft.turbo.base.utils.RemoteCallWrapper;
import cn.kaziki.nft.turbo.order.OrderException;
import cn.kaziki.nft.turbo.order.sharding.id.DistributeID;
import cn.kaziki.nft.turbo.order.sharding.id.WorkerIdHolder;
import cn.kaziki.nft.turbo.order.validator.OrderCreateValidator;
import cn.kaziki.nft.turbo.trade.application.TradeApplicationService;
import cn.kaziki.nft.turbo.trade.exception.TradeErrorCode;
import cn.kaziki.nft.turbo.trade.exception.TradeException;
import cn.kaziki.nft.turbo.trade.param.BookParam;
import cn.kaziki.nft.turbo.trade.param.BuyParam;
import cn.kaziki.nft.turbo.trade.param.CancelParam;
import cn.kaziki.nft.turbo.trade.param.PayParam;
import cn.kaziki.nft.turbo.web.vo.Result;
import cn.kaziki.turbo.stream.producer.StreamProducer;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.fastjson2.JSON;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import static cn.kaziki.nft.turbo.web.filter.TokenFilter.TOKEN_THREAD_LOCAL;

import static cn.kaziki.nft.turbo.api.common.constant.CommonConstant.SEPARATOR;
import static cn.kaziki.nft.turbo.api.user.constant.UserType.PLATFORM;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("trade")
public class TradeController {

    private static ThreadFactory inventoryBypassVerifyThreadFactory = new ThreadFactoryBuilder()
            .setNameFormat("inventory-bypass-verify-pool-%d").build();

    private ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(10, inventoryBypassVerifyThreadFactory);

    @Autowired
    private GoodsFacadeService goodsFacadeService;
    @Autowired
    private OrderFacadeService orderFacadeService;
    @Autowired
    private InventoryFacadeService inventoryFacadeService;
    @DubboReference(version = "1.0.0")
    private InventoryCheckFacadeService inventoryCheckFacadeService;
    @Autowired
    private PayFacadeService payFacadeService;
    @Autowired
    private StreamProducer streamProducer;
    @Autowired
    private OrderCreateValidator orderValidatorChain;
    @Autowired
    private TradeApplicationService tradeApplicationService;

    // 下单
    @PostMapping("/buy")
    @SentinelResource(value = "/trade/buy")
    public Result<String> buy(@Valid @RequestBody BuyParam buyParam) {
        try {
            String userId = (String) StpUtil.getLoginId();
            // 获取创建订单请求参数,订单号:业务id++sequence++用户i
            OrderCreateRequest request = getOrderCreateRequest(buyParam);
            // 创建订单,推进订单到create状态,发送一个半事务,确认订单状态到confirm状态
            OrderResponse response = RemoteCallWrapper.call(req -> orderFacadeService.create(req), request, "createOrder");
            if(response.getSuccess()){
                InventoryRequest inventoryRequest = new InventoryRequest(request);
                inventoryBypassVerify(inventoryRequest);
                return Result.success(request.getOrderId());
            }
        }catch (OrderException | TradeException e){
            return Result.error(e.getErrorCode().getCode(), e.getErrorCode().getMessage());
        }catch (Exception e){
            log.error(e.getMessage());
        }
        return Result.error(TradeErrorCode.ORDER_CREATE_FAILED.getCode(), TradeErrorCode.ORDER_CREATE_FAILED.getMessage());
    }

    // 下单 MQ
    @PostMapping("/newBuy")
    @SentinelResource(value = "/trade/buy")
    public Result<String> newBuy(@Valid @RequestBody BuyParam buyParam) {
        OrderCreateRequest orderCreateRequest = null;

        try {
            // 1.构建创建订单参数，前置校验
            orderCreateRequest = getOrderCreateRequest(buyParam);
            orderCreateRequest.setExtendInfo("用户下单（热点商品）");
            //责任链校验1.查询用户是否有购买权限,2.查询用户是否是正常状态3.查询商品是否存在,可售,价格是否变化4.
            orderValidatorChain.validate(orderCreateRequest);

            // 2.发送消息；执行本地事务--扣减redis库存并添加一条记录；
            //本地事务执行器：InventoryDecreaseTransactionListener  消息监听：NewBuyMsgListener or NewBuyBatchMsgListener
            boolean result = streamProducer.send("newBuy-out-0", buyParam.getGoodsType(), JSON.toJSONString(orderCreateRequest));

            // 这里的result表示消息是否发出去了，而不是本地事务是否执行成功
            if (!result) {
                throw new TradeException(TradeErrorCode.ORDER_CREATE_FAILED);
            }

            /**
             * 情况一：redis库存扣减失败，此次下单失败，结束
             *        redis库存扣减成功，记录decrease log，发送远程事务消息（扣减DB库存，创建并确认订单）
             * 情况二：DB库存扣减失败（限流、DB异常），前端轮询不到订单，下单失败
             *        DB库存扣减成功，创建订单
             * 情况三：DB创建订单失败，回滚库存，删除decrease log，increase log，前端轮询不到订单，下单失败
             */

            // 3.验证下单：查 Redis 扣减记录，存在则视为下单成功（由旁路验证确认 DB 一致性）
            InventoryRequest inventoryRequest = new InventoryRequest(orderCreateRequest);
            SingleResponse<String> response = inventoryFacadeService.getInventoryDecreaseLog(inventoryRequest);

            if (response.getSuccess() && response.getData() != null) {
                // Redis 扣减成功，异步等待 MQ 消费完成，旁路验证清理
                inventoryBypassVerify(inventoryRequest);
                return Result.success(orderCreateRequest.getOrderId());
            }
            log.error("decrease log not found, orderId={}", orderCreateRequest.getOrderId());
            return Result.error(TradeErrorCode.ORDER_CREATE_FAILED.getCode(), "订单创建失败，请重试");

        } catch (OrderException | TradeException e) {
            return Result.error(e.getErrorCode().getCode(), e.getErrorCode().getMessage());
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        return Result.error(TradeErrorCode.ORDER_CREATE_FAILED.getCode(), TradeErrorCode.ORDER_CREATE_FAILED.getMessage());
    }

    // DB库存扣减旁路验证
    private void inventoryBypassVerify(InventoryRequest inventoryRequest) {
        try {
            // 延迟3秒检查是否有DB商品库存扣减记录
            scheduler.schedule(() -> {
                InventoryCheckRequest inventoryCheckRequest = new InventoryCheckRequest();
                inventoryCheckRequest.setIdentifier(inventoryRequest.getIdentifier());
                inventoryCheckRequest.setGoodsType(inventoryRequest.getGoodsType());
                inventoryCheckRequest.setGoodsId(inventoryRequest.getGoodsId());
                inventoryCheckRequest.setGoodsEvent(GoodsEvent.TRY_SALE);
                inventoryCheckRequest.setChangedQuantity(inventoryRequest.getInventory());
                InventoryCheckResponse checkResponse = inventoryCheckFacadeService.check(inventoryCheckRequest);
                // 核验成功,数据一致
                if (checkResponse.getSuccess() && checkResponse.getCheckResult()) {
                    // 删除redis扣减记录
                    inventoryFacadeService.removeInventoryDecreaseLog(inventoryRequest);
                }
            }, 3, TimeUnit.SECONDS);

        } catch (Exception e) {
            //核验失败打印日志，不影响主流程，等异步任务再核对
            log.error("inventoryBypassVerify failed,", e);
        }
    }

    // 普通下单，非热点商品
    @PostMapping("/normalBuy")
    @SentinelResource(value = "/trade/normalBuy")
    public Result<String> normalBuy(@Valid @RequestBody BuyParam buyParam) {
        try {
            OrderCreateAndConfirmRequest orderCreateAndConfirmRequest = getOrderCreateAndConfirmRequest(buyParam);
            // 1.前置校验
            orderValidatorChain.validate(orderCreateAndConfirmRequest);
            // 2.下单
            OrderResponse orderResponse = RemoteCallWrapper.call(req -> tradeApplicationService.normalBuy(req), orderCreateAndConfirmRequest, "createOrder");
            // 3.更新缓存
            if (orderResponse.getSuccess()) {
                //同步写redis，如果失败，不阻塞流程，靠binlog同步保障
                try {
                    InventoryRequest inventoryRequest = new InventoryRequest(orderCreateAndConfirmRequest);
                    inventoryFacadeService.decrease(inventoryRequest);
                } catch (Exception e) {
                    log.error("decrease inventory from redis failed", e);
                }

                return Result.success(orderCreateAndConfirmRequest.getOrderId());
            }
        } catch (OrderException | TradeException e) {
            return Result.error(e.getErrorCode().getCode(), e.getErrorCode().getMessage());
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        throw new TradeException(TradeErrorCode.ORDER_CREATE_FAILED);
    }

    // 取消订单
    @PostMapping("/cancel")
    public Result<Boolean> cancel(@Valid @RequestBody CancelParam cancelParam) {
        String userId = (String) StpUtil.getLoginId();
        // 组装取消订单参数
        OrderCancelRequest orderCancelRequest = new OrderCancelRequest();
        orderCancelRequest.setIdentifier(cancelParam.getOrderId());
        orderCancelRequest.setOperateTime(new Date());
        orderCancelRequest.setOrderId(cancelParam.getOrderId());
        orderCancelRequest.setOperator(userId);
        orderCancelRequest.setOperatorType(UserType.CUSTOMER);
        // 远程调用order服务
        OrderResponse orderResponse = RemoteCallWrapper.call(req -> orderFacadeService.cancel(req), orderCancelRequest, "cancelOrder");

        if (orderResponse.getSuccess()) {
            return Result.success(true);
        }

        throw new TradeException(TradeErrorCode.ORDER_CANCEL_FAILED);
    }


    @NotNull
    private OrderCreateRequest getOrderCreateRequest(BuyParam buyParam) {
        String userId = (String) StpUtil.getLoginId();
        // 生成订单号,订单号:业务id++sequence++用户id
        String orderId = DistributeID.generateWithSnowflake(BusinessCode.TRADE_ORDER, WorkerIdHolder.WORKER_ID, userId);
        //创建订单
        OrderCreateRequest orderCreateRequest = new OrderCreateRequest();
        orderCreateRequest.setOrderId(orderId);
        orderCreateRequest.setIdentifier(TOKEN_THREAD_LOCAL.get());
        orderCreateRequest.setBuyerId(userId);
        orderCreateRequest.setGoodsId(buyParam.getGoodsId());
        orderCreateRequest.setGoodsType(GoodsType.valueOf(buyParam.getGoodsType()));
        orderCreateRequest.setItemCount(buyParam.getItemCount());
        // 校验商品是否存在
        // 校验商品是否可售
        // 校验商品库存是否足够
        BaseGoodsVO goodsVO = goodsFacadeService.getGoods(buyParam.getGoodsId(), GoodsType.valueOf(buyParam.getGoodsType()));
        if (goodsVO == null || !goodsVO.available()) {
            throw new TradeException(TradeErrorCode.GOODS_NOT_FOR_SALE);
        }
        orderCreateRequest.setItemPrice(goodsVO.getPrice());
        orderCreateRequest.setSellerId(goodsVO.getSellerId());
        orderCreateRequest.setGoodsName(goodsVO.getGoodsName());
        orderCreateRequest.setGoodsPicUrl(goodsVO.getGoodsPicUrl());
        orderCreateRequest.setSnapshotVersion(goodsVO.getVersion());
        orderCreateRequest.setOrderAmount(orderCreateRequest.getItemPrice().multiply(new BigDecimal(orderCreateRequest.getItemCount())));

        return orderCreateRequest;
    }

    @NotNull
    private OrderCreateAndConfirmRequest getOrderCreateAndConfirmRequest(BuyParam buyParam) {
        String userId = (String) StpUtil.getLoginId();
        String orderId = DistributeID.generateWithSnowflake(BusinessCode.TRADE_ORDER, WorkerIdHolder.WORKER_ID, userId);
        //创建订单
        OrderCreateAndConfirmRequest orderCreateAndConfirmRequest = new OrderCreateAndConfirmRequest();
        orderCreateAndConfirmRequest.setOrderId(orderId);
        orderCreateAndConfirmRequest.setIdentifier(TOKEN_THREAD_LOCAL.get());
        orderCreateAndConfirmRequest.setBuyerId(userId);
        orderCreateAndConfirmRequest.setGoodsId(buyParam.getGoodsId());
        orderCreateAndConfirmRequest.setGoodsType(GoodsType.valueOf(buyParam.getGoodsType()));
        orderCreateAndConfirmRequest.setItemCount(buyParam.getItemCount());
        BaseGoodsVO goodsVO = goodsFacadeService.getGoods(buyParam.getGoodsId(), GoodsType.valueOf(buyParam.getGoodsType()));
        if (goodsVO == null || !goodsVO.available()) {
            throw new TradeException(TradeErrorCode.GOODS_NOT_FOR_SALE);
        }
        orderCreateAndConfirmRequest.setItemPrice(goodsVO.getPrice());
        orderCreateAndConfirmRequest.setSellerId(goodsVO.getSellerId());
        orderCreateAndConfirmRequest.setGoodsName(goodsVO.getGoodsName());
        orderCreateAndConfirmRequest.setGoodsPicUrl(goodsVO.getGoodsPicUrl());
        orderCreateAndConfirmRequest.setSnapshotVersion(goodsVO.getVersion());
        orderCreateAndConfirmRequest.setOrderAmount(orderCreateAndConfirmRequest.getItemPrice().multiply(new BigDecimal(orderCreateAndConfirmRequest.getItemCount())));
        orderCreateAndConfirmRequest.setOperator(UserType.PLATFORM.name());
        orderCreateAndConfirmRequest.setOperatorType(UserType.PLATFORM);
        orderCreateAndConfirmRequest.setOperateTime(new Date());
        return orderCreateAndConfirmRequest;
    }

    // 支付
    @PostMapping("/pay")
    public Result<PayOrderVO> pay(@Valid @RequestBody PayParam payParam) {
        String userId = (String) StpUtil.getLoginId();
        SingleResponse<TradeOrderVO> singleResponse = orderFacadeService.getTradeOrder(payParam.getOrderId(), userId);

        TradeOrderVO tradeOrderVO = singleResponse.getData();

        if (tradeOrderVO == null) {
            throw new TradeException(TradeErrorCode.GOODS_NOT_EXIST);
        }

        if (tradeOrderVO.getOrderState() != TradeOrderState.CONFIRM) {
            throw new TradeException(TradeErrorCode.ORDER_IS_CANNOT_PAY);
        }

        // 检测订单是否超时
        if (tradeOrderVO.getTimeout()) {
            doAsyncTimeoutOrder(tradeOrderVO);
            throw new TradeException(TradeErrorCode.ORDER_IS_CANNOT_PAY);
        }

        // 创建支付单
        PayCreateRequest payCreateRequest = new PayCreateRequest();
        payCreateRequest.setOrderAmount(tradeOrderVO.getOrderAmount());
        payCreateRequest.setBizNo(tradeOrderVO.getOrderId());
        payCreateRequest.setBizType(BizOrderType.TRADE_ORDER);
        payCreateRequest.setMemo(tradeOrderVO.getGoodsName());
        payCreateRequest.setPayChannel(payParam.getPayChannel());
        payCreateRequest.setPayerId(tradeOrderVO.getBuyerId());
        payCreateRequest.setPayerType(tradeOrderVO.getBuyerType());
        payCreateRequest.setPayeeId(tradeOrderVO.getSellerId());
        payCreateRequest.setPayeeType(tradeOrderVO.getSellerType());
        // 远程调用支付服务，获取支付地址
        PayCreateResponse payCreateResponse = RemoteCallWrapper.call(req -> payFacadeService.generatePayUrl(req), payCreateRequest, "generatePayUrl");

        if (payCreateResponse.getSuccess()) {
            PayOrderVO payOrderVO = new PayOrderVO();
            payOrderVO.setPayOrderId(payCreateResponse.getPayOrderId());
            payOrderVO.setPayUrl(payCreateResponse.getPayUrl());
            return Result.success(payOrderVO);
        }

        throw new TradeException(TradeErrorCode.PAY_CREATE_FAILED);
    }

    private void doAsyncTimeoutOrder(TradeOrderVO tradeOrderVO) {
        if (tradeOrderVO.getOrderState() != TradeOrderState.CLOSED) {
            Thread.ofVirtual().start(() -> {
                OrderTimeoutRequest cancelRequest = new OrderTimeoutRequest();
                cancelRequest.setOperatorType(PLATFORM);
                cancelRequest.setOperator(PLATFORM.getDesc());
                cancelRequest.setOrderId(tradeOrderVO.getOrderId());
                cancelRequest.setOperateTime(new Date());
                cancelRequest.setIdentifier(UUID.randomUUID().toString());
                orderFacadeService.timeout(cancelRequest);
            });
        }
    }

    // 预约商品
    @PostMapping("/book")
    public Result<Long> book(@Valid @RequestBody BookParam bookParam) {
        String userId = (String) StpUtil.getLoginId();

        GoodsBookRequest goodsBookRequest = new GoodsBookRequest();
        goodsBookRequest.setGoodsId(bookParam.getGoodsId());
        goodsBookRequest.setGoodsType(GoodsType.valueOf(bookParam.getGoodsType()));
        //数藏比较特殊，一个商品只能预定一次，所以这里直接用userId+goodsType+goodsId作为标识了，如果支持多次预定的话，需要在再有个活动的概念，基于活动做预约
        goodsBookRequest.setIdentifier(userId + SEPARATOR + bookParam.getGoodsType() + SEPARATOR + bookParam.getGoodsId());
        goodsBookRequest.setBuyerId(userId);
        GoodsBookResponse goodsBookResponse = RemoteCallWrapper.call(req -> goodsFacadeService.book(req), goodsBookRequest, "bookGoods");
        if (goodsBookResponse.getSuccess()) {
            return Result.success(goodsBookResponse.getBookId());
        }
        throw new TradeException(TradeErrorCode.GOODS_BOOK_FAILED);
    }

}
