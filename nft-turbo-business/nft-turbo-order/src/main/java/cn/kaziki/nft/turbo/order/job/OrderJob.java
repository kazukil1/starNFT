package cn.kaziki.nft.turbo.order.job;

import cn.kaziki.nft.turbo.api.common.constant.BizOrderType;
import cn.kaziki.nft.turbo.api.common.constant.BusinessCode;
import cn.kaziki.nft.turbo.api.order.service.OrderFacadeService;
import cn.kaziki.nft.turbo.api.order.request.OrderConfirmRequest;
import cn.kaziki.nft.turbo.api.order.request.OrderTimeoutRequest;
import cn.kaziki.nft.turbo.api.pay.constant.PayOrderState;
import cn.kaziki.nft.turbo.api.pay.model.PayOrderVO;
import cn.kaziki.nft.turbo.api.pay.request.PayQueryByBizNo;
import cn.kaziki.nft.turbo.api.pay.request.PayQueryRequest;
import cn.kaziki.nft.turbo.api.pay.service.PayFacadeService;
import cn.kaziki.nft.turbo.api.user.constant.UserType;
import cn.kaziki.nft.turbo.base.response.MultiResponse;
import cn.kaziki.nft.turbo.order.domain.entity.TradeOrder;
import cn.kaziki.nft.turbo.order.domain.service.OrderReadService;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shardingsphere.infra.hint.HintManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * 订单相关的 XXL-Job 定时任务处理器
 * <p>
 * 主要职责：
 * 1. {@link #orderTimeOutExecute()}    —— 扫描超时未支付的订单并执行关单（释放库存/退款）
 * 2. {@link #orderConfirmExecute()}    —— 扫描待确认的订单并自动确认（发货后 N 天未主动确认收货的订单）
 * <p>
 * 设计要点：
 * <ul>
 *   <li>使用 XXL-Job 的<strong>分片广播</strong>能力，按 buyerId 末两位（00~99 共 100 组）对分片总数做取模，
 *       保证不同任务实例处理不同的尾号分组，避免重复扫描同一张表的同一批数据。</li>
 *   <li>每个分片内部采用<strong>生产者-消费者</strong>模式：主线程作为生产者按游标（上一页最大 ID + 1）分页查询订单，
 *       避免深分页性能问题，将结果放入 {@link BlockingQueue}；ForkJoinPool 子线程作为消费者并发取单执行操作。</li>
 *   <li>通过 {@link #POISON}（毒丸对象）作为队列结束信号。毒丸本身不是有效订单数据，消费者遇到它就退出当前处理循环。</li>
 * </ul>
 * <p>
 * 废弃的 {@code WithHint} 系列方法保留仅供参考，新方案统一使用应用层分片。
 */
@Component
public class OrderJob {

    @Autowired
    private OrderFacadeService orderFacadeService;

    @Autowired
    private OrderReadService orderReadService;

    @Autowired
    private PayFacadeService payFacadeService;

    /** 队列容量，单分片最多缓冲 2000 条订单 */
    private static final int CAPACITY = 2000;

    /** 订单超时关单任务的缓冲队列 */
    private final BlockingQueue<TradeOrder> orderTimeoutBlockingQueue = new LinkedBlockingQueue<>(CAPACITY);

    /** 订单自动确认任务的缓冲队列 */
    private final BlockingQueue<TradeOrder> orderConfirmBlockingQueue = new LinkedBlockingQueue<>(CAPACITY);

    /** 并发处理线程池，用于异步消费队列中的订单，并行度为 10 */
    private final ForkJoinPool forkJoinPool = new ForkJoinPool(10);

    /** 分页查询时每页大小 */
    private static final int PAGE_SIZE = 500;

    private static final Logger LOG = LoggerFactory.getLogger(OrderJob.class);

    /** 毒丸对象 —— 放入队列后消费者遇到它就结束当前分片的处理循环；本身非有效订单，仅作哨兵使用 */
    private static final TradeOrder POISON = new TradeOrder();

    /** buyerId 末两位的最大值，即 00~99 共 100 个用户分组 */
    private static final int MAX_TAIL_NUMBER = 99;

    /**
     * XXL-Job 任务：订单超时关单
     * <p>
     * 执行流程：
     * <ol>
     *   <li>获取当前分片索引与总分片数，计算出当前实例需要处理的 buyerId 尾号列表</li>
     *   <li>对每个尾号分页查询超时订单，放入阻塞队列，并提交一个消费者线程</li>
     *   <li>分页查询直到无更多数据，放入 POISON 毒丸通知消费者结束</li>
     * </ol>
     *
     * @return 任务执行结果
     */
    @XxlJob("orderTimeOutExecute")
    public ReturnT<String> orderTimeOutExecute() {
        try {
            // 获取当前分片索引和总分片数：同一任务的 N 个实例分别处理 shardIndex=0..N-1
            int shardIndex = XxlJobHelper.getShardIndex();
            int shardTotal = XxlJobHelper.getShardTotal();

            LOG.info("orderTimeOutExecute start to execute , shardIndex is {} , shardTotal is {}", shardIndex, shardTotal);

            // 计算当前分片需要覆盖的 buyerId 尾号集合，保证所有尾号都有且仅有一个实例处理
            List<String> buyerIdTailNumberList = new ArrayList<>();
            for (int i = 0; i <= MAX_TAIL_NUMBER; i++) {
                if (i % shardTotal == shardIndex) {
                    buyerIdTailNumberList.add(StringUtils.leftPad(String.valueOf(i), 2, "0"));
                }
            }

            buyerIdTailNumberList.forEach(buyerIdTailNumber -> {
                try {
                    // [TODO] addAll 在队列满时会抛 IllegalStateException，改为 put() 阻塞写入更安全
                    //获取当前尾号的超时订单
                    List<TradeOrder> tradeOrders = orderReadService.pageQueryTimeoutOrders(PAGE_SIZE, buyerIdTailNumber, null);
                    orderTimeoutBlockingQueue.addAll(tradeOrders);
                    forkJoinPool.execute(this::executeTimeout);
                    // 游标式分页：以上一页最大 ID + 1 作为下一页起点，避免 offset 深分页
                    while (CollectionUtils.isNotEmpty(tradeOrders)) {
                        long maxId = tradeOrders.stream().mapToLong(TradeOrder::getId).max().orElse(Long.MAX_VALUE);
                        tradeOrders = orderReadService.pageQueryTimeoutOrders(PAGE_SIZE, buyerIdTailNumber, maxId + 1);
                        orderTimeoutBlockingQueue.addAll(tradeOrders);
                    }
                } finally {
                    orderTimeoutBlockingQueue.add(POISON);
                    LOG.debug("POISON added to blocking queue ，buyerIdTailNumber is {}", buyerIdTailNumber);
                }
            });

            return ReturnT.SUCCESS;
        } catch (Exception e) {
            LOG.error("orderTimeOutExecute failed", e);
            // 重新抛出让 XXL-Job 框架感知任务失败并触发告警
            throw e;
        }
    }

    /**
     * 消费者线程：从 {@link #orderTimeoutBlockingQueue} 中循环取订单并执行超时关单，
     * 直到取到 {@link #POISON} 毒丸后退出。
     * <p>
     * 使用 {@link BlockingQueue#take()} 阻塞等待，省去自旋轮询开销。
     * 注意：该方法被多个 ForkJoinPool worker 并发调用，因此多个消费者会各自阻塞在同一个队列上抢占消费。
     */
    private void executeTimeout() {
        TradeOrder tradeOrder = null;
        try {
            while (true) {
                tradeOrder = orderTimeoutBlockingQueue.take();
                if (tradeOrder == POISON) {
                    LOG.debug("POISON toked from blocking queue");
                    break;
                }
                LOG.info("executeTimeout tradeOrderId = {}", tradeOrder.getId());
                executeTimeoutSingle(tradeOrder);
            }
        } catch (InterruptedException e) {
            LOG.error("executeTimeout failed", e);
        }
        LOG.debug("executeTimeout finish");
    }

    /**
     * 对单个订单执行超时关单逻辑。
     * <p>
     * 关单前先到支付中心查询是否存在 PAID 状态的支付单：
     * <ul>
     *   <li>存在 PAID 支付单 → 用户已付款，不能关单（避免退款纠纷），跳过</li>
     *   <li>不存在 PAID 支付单 → 用户确实未付款，调用 {@code orderFacadeService.timeout} 关单释放库存</li>
     * </ul>
     * 这种双写校验是为了应对定时任务执行延迟或支付回调乱序等边缘场景。
     *
     * @param tradeOrder 待处理的交易订单
     */
    private void executeTimeoutSingle(TradeOrder tradeOrder) {
        // 构造查询条件：查询该订单是否有 PAID 状态的支付单
        PayQueryRequest request = new PayQueryRequest();
        request.setPayerId(tradeOrder.getBuyerId());
        request.setPayOrderState(PayOrderState.PAID);
        PayQueryByBizNo payQueryByBizNo = new PayQueryByBizNo();
        payQueryByBizNo.setBizNo(tradeOrder.getOrderId());
        payQueryByBizNo.setBizType(BizOrderType.TRADE_ORDER.name());
        request.setPayQueryCondition(payQueryByBizNo);
        MultiResponse<PayOrderVO> payQueryResponse = payFacadeService.queryPayOrders(request);
        // 远程调用成功并且获取不到支付单，进行订单超时处理
        if (payQueryResponse.getSuccess() && CollectionUtils.isEmpty(payQueryResponse.getDatas())) {
            LOG.info("start to execute order timeout , orderId is {}", tradeOrder.getOrderId());
            OrderTimeoutRequest orderTimeoutRequest = new OrderTimeoutRequest();
            orderTimeoutRequest.setOrderId(tradeOrder.getOrderId());
            orderTimeoutRequest.setOperateTime(new Date());
            orderTimeoutRequest.setOperator(UserType.PLATFORM.name());
            orderTimeoutRequest.setOperatorType(UserType.PLATFORM);
            orderTimeoutRequest.setIdentifier(tradeOrder.getOrderId());
            orderFacadeService.timeout(orderTimeoutRequest);
        }
    }

    /**
     * XXL-Job 任务：订单自动确认
     * <p>
     * 逻辑结构与 {@link #orderTimeOutExecute()} 完全一致，
     * 区别仅在于查询的是"待确认"状态的订单，而不是"超时未支付"的订单。
     *
     * @return 任务执行结果
     */
    @XxlJob("orderConfirmExecute")
    public ReturnT<String> orderConfirmExecute() {

        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();

        LOG.info("orderConfirmExecute start to execute , shardIndex is {} , shardTotal is {}", shardIndex, shardTotal);

        List<String> buyerIdTailNumberList = new ArrayList<>();
        for (int i = 0; i <= MAX_TAIL_NUMBER; i++) {
            if (i % shardTotal == shardIndex) {
                buyerIdTailNumberList.add(StringUtils.leftPad(String.valueOf(i), 2, "0"));
            }
        }

        buyerIdTailNumberList.forEach(buyerIdTailNumber -> {
            try {
                List<TradeOrder> tradeOrders = orderReadService.pageQueryNeedConfirmOrders(PAGE_SIZE, buyerIdTailNumber, null);
                orderConfirmBlockingQueue.addAll(tradeOrders);
                forkJoinPool.execute(this::executeConfirm);

                while (CollectionUtils.isNotEmpty(tradeOrders)) {
                    long maxId = tradeOrders.stream().mapToLong(TradeOrder::getId).max().orElse(Long.MAX_VALUE);
                    tradeOrders = orderReadService.pageQueryNeedConfirmOrders(PAGE_SIZE, buyerIdTailNumber, maxId + 1);
                    orderConfirmBlockingQueue.addAll(tradeOrders);
                }
            } finally {
                orderConfirmBlockingQueue.add(POISON);
                LOG.debug("POISON added to blocking queue ，buyerIdTailNumber is {}", buyerIdTailNumber);
            }
        });

        return ReturnT.SUCCESS;
    }

    /**
     * 消费者线程：从 {@link #orderConfirmBlockingQueue} 中循环取订单并执行自动确认，
     * 直到取到 {@link #POISON} 毒丸后退出。
     */
    private void executeConfirm() {
        TradeOrder tradeOrder = null;
        try {
            while (true) {
                tradeOrder = orderConfirmBlockingQueue.take();
                if (tradeOrder == POISON) {
                    LOG.debug("POISON toked from blocking queue");
                    break;
                }
                executeConfirmSingle(tradeOrder);
            }
        } catch (InterruptedException e) {
            LOG.error("executeConfirm failed", e);
        }
        LOG.debug("executeConfirm finish");
    }

    /**
     * 【已废弃】基于 ShardingSphere Hint 强制路由到物理分片表的超时关单方案。
     * <p>
     * 废弃原因：
     * <ol>
     *   <li>{@link HintManager} 基于 ThreadLocal，并发场景下若 {@code close()} 遗漏会导致上下文泄漏，影响后续 SQL 路由。</li>
     *   <li>Hint 方式需要对每张物理表全表扫描，无法利用业务字段（如 buyerId 尾号）做窄范围过滤，数据量大时性能差。</li>
     *   <li>现方案改为按 buyerId 末两位做应用层分片（见 {@link #orderTimeOutExecute()}），可复用业务索引且天然避免 ThreadLocal 污染。</li>
     * </ol>
     */
    @XxlJob("orderTimeOutExecuteWithHint")
    @Deprecated
    public ReturnT<String> orderTimeOutExecuteWithHint() {
        try {
            int shardIndex = XxlJobHelper.getShardIndex();
            int shardTotal = XxlJobHelper.getShardTotal();

            LOG.info("orderTimeOutExecute start to execute , shardIndex is {} , shardTotal is {}", shardIndex, shardTotal);

            int shardingTableCount = BusinessCode.TRADE_ORDER.tableCount();

            if (shardIndex >= shardingTableCount) {
                return ReturnT.SUCCESS;
            }

            List<Integer> shardingTableIndexes = new ArrayList<>();
            for (int realTableIndex = 0; realTableIndex < shardingTableCount; realTableIndex++) {
                if (realTableIndex % shardTotal == shardIndex) {
                    shardingTableIndexes.add(realTableIndex);
                }
            }

            shardingTableIndexes.forEach(index -> {

                try (HintManager hintManager = HintManager.getInstance()) {
                    LOG.info("shardIndex {} is execute", index);
                    hintManager.addTableShardingValue("trade_order", "000" + index);
                    List<TradeOrder> tradeOrders = orderReadService.pageQueryTimeoutOrders(PAGE_SIZE, null, null);

                    while (CollectionUtils.isNotEmpty(tradeOrders)) {
                        tradeOrders.forEach(this::executeTimeoutSingle);
                        long maxId = tradeOrders.stream().mapToLong(TradeOrder::getId).max().orElse(Long.MAX_VALUE);
                        tradeOrders = orderReadService.pageQueryTimeoutOrders(PAGE_SIZE, null, maxId + 1);
                    }
                }
            });

            return ReturnT.SUCCESS;
        } catch (Exception e) {
            LOG.error("orderTimeOutExecute failed", e);
            throw e;
        }
    }

    /**
     * 【已废弃】基于 ShardingSphere Hint 的自动确认方案，
     * 废弃原因同 {@link #orderTimeOutExecuteWithHint()}。
     */
    @XxlJob("orderConfirmExecuteWithHint")
    @Deprecated
    public ReturnT<String> orderConfirmExecuteWithHint() {

        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();

        int shardingTableCount = BusinessCode.TRADE_ORDER.tableCount();

        if (shardIndex >= shardingTableCount) {
            return ReturnT.SUCCESS;
        }

        List<Integer> shardingTableIndexes = new ArrayList<>();
        for (int realTableIndex = 0; realTableIndex < shardingTableCount; realTableIndex++) {
            if (realTableIndex % shardTotal == shardIndex) {
                shardingTableIndexes.add(realTableIndex);
            }
        }

        shardingTableIndexes.parallelStream().forEach(index -> {
            HintManager hintManager = HintManager.getInstance();
            hintManager.addTableShardingValue("trade_order", "000" + index);
            List<TradeOrder> tradeOrders = orderReadService.pageQueryNeedConfirmOrders(PAGE_SIZE, null, null);
            while (CollectionUtils.isNotEmpty(tradeOrders)) {
                tradeOrders.forEach(this::executeConfirmSingle);
                long maxId = tradeOrders.stream().mapToLong(TradeOrder::getId).max().orElse(Long.MAX_VALUE);
                tradeOrders = orderReadService.pageQueryNeedConfirmOrders(PAGE_SIZE, null, maxId + 1);
            }
        });

        return ReturnT.SUCCESS;
    }

    /**
     * 对单个订单执行自动确认，将订单流转到"已完成"状态。
     * 操作人固定为平台（PLATFORM），代表系统自动触发。
     *
     * @param tradeOrder 待确认的交易订单
     */
    private void executeConfirmSingle(TradeOrder tradeOrder) {
        OrderConfirmRequest confirmRequest = new OrderConfirmRequest();
        confirmRequest.setOperator(UserType.PLATFORM.name());
        confirmRequest.setOperatorType(UserType.PLATFORM);
        confirmRequest.setOrderId(tradeOrder.getOrderId());
        confirmRequest.setIdentifier(tradeOrder.getIdentifier());
        confirmRequest.setOperateTime(new Date());
        confirmRequest.setOrderId(tradeOrder.getOrderId());
        confirmRequest.setBuyerId(tradeOrder.getBuyerId());
        confirmRequest.setItemCount(tradeOrder.getItemCount());
        confirmRequest.setGoodsId(tradeOrder.getGoodsId());
        confirmRequest.setGoodsType(tradeOrder.getGoodsType());
        orderFacadeService.confirm(confirmRequest);
    }
}