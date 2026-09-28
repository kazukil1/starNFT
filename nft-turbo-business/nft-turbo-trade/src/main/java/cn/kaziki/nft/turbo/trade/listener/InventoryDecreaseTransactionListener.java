package cn.kaziki.nft.turbo.trade.listener;

import cn.kaziki.nft.turbo.api.inventory.request.InventoryRequest;
import cn.kaziki.nft.turbo.api.inventory.service.InventoryFacadeService;
import cn.kaziki.nft.turbo.api.order.request.OrderCreateRequest;
import cn.kaziki.nft.turbo.base.response.SingleResponse;
import com.alibaba.fastjson2.JSON;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 用户下单扣减缓存库存 本地事务
 */
@Component
public class InventoryDecreaseTransactionListener implements TransactionListener {

    private static final Logger logger = LoggerFactory.getLogger(InventoryDecreaseTransactionListener.class);

    @Autowired
    private InventoryFacadeService inventoryFacadeService;

    @Override
    public LocalTransactionState executeLocalTransaction(Message message, Object o) {
        try {
            // 1.从消息获取创建订单参数，转换为库存请求
            OrderCreateRequest orderCreateRequest = JSON.parseObject(JSON.parseObject(message.getBody()).getString("body"), OrderCreateRequest.class);
            InventoryRequest inventoryRequest = new InventoryRequest(orderCreateRequest);
            // 2.缓存预扣减库存
            SingleResponse<Boolean> response = inventoryFacadeService.decrease(inventoryRequest);
            // 3.本地事务执行完成，发送消息执行远程事务（NewBuyMsgListener）
            if (response.getSuccess() && response.getData()) {
                return LocalTransactionState.COMMIT_MESSAGE;
            } else {
                return LocalTransactionState.ROLLBACK_MESSAGE;
            }
        } catch (Exception e) {
            logger.error("executeLocalTransaction error, message = {}", message, e);
            return LocalTransactionState.ROLLBACK_MESSAGE;
        }
    }

    /**
     * RocketMQ 事务回查接口
     * 当 Broker 长时间未收到本地事务的执行结果时，会触发该方法进行事务状态回查。
     * 核心逻辑：通过查询库存扣减日志来判断之前的本地事务（缓存库存预扣减）是否真正执行成功。
     *
     * @param messageExt Broker 回查时携带的事务消息，body 中包含订单创建请求信息
     * @return COMMIT_MESSAGE  表示本地事务成功，Broker 将提交该消息
     *         ROLLBACK_MESSAGE 表示本地事务失败或不存在，Broker 将回滚该消息
     */
    @Override
    public LocalTransactionState checkLocalTransaction(MessageExt messageExt) {
        // 1. 解析回查消息体，提取订单创建请求参数
        OrderCreateRequest orderCreateRequest = JSON.parseObject(JSON.parseObject(new String(messageExt.getBody())).getString("body"), OrderCreateRequest.class);
        // 2. 基于订单参数构造库存查询请求
        InventoryRequest inventoryRequest = new InventoryRequest(orderCreateRequest);
        // 3. 查询库存扣减日志，判断预扣减是否真的落库
        SingleResponse<String> response = inventoryFacadeService.getInventoryDecreaseLog(inventoryRequest);
        // 4. 若日志存在且查询成功，说明本地事务已提交；否则视为需要回滚
        return response.getSuccess() && response.getData() != null ? LocalTransactionState.COMMIT_MESSAGE : LocalTransactionState.ROLLBACK_MESSAGE;
    }
}