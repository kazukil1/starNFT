# RocketMQ 事务消息发送报错 No route info of this topic

## 现象

调用订单取消接口时，`StreamProducer.send()` 通过 `StreamBridge` 发送事务消息到 `orderClose-out-0` binding，RocketMQ 抛出异常：

```
org.apache.rocketmq.client.exception.MQClientException: No route info of this topic: order-close-topic
```

完整调用链：

```
TradeController.cancel()
  → OrderFacadeService.cancel()                          [Dubbo RPC]
    → OrderFacadeServiceImpl.sendTransactionMsgForClose()
      → StreamProducer.send("orderClose-out-0", ...)     [StreamBridge.send()]
        → RocketMQProducerMessageHandler.handleMessageInternal()
          → TransactionMQProducer.sendMessageInTransaction()
            → MQClientException: No route info
```

## 根因

两个问题叠加导致 broker 没有在 namesrv 正确注册：

### 1. broker 连不上 namesrv

`docker-compose.yml` 中 broker 启动命令：

```yaml
# 错误配置
command: sh mqbroker -n 127.0.0.1:9876 -c /home/rocketmq/rocketmq-5.2.0/conf/broker.conf
```

`-n 127.0.0.1:9876` 让 broker 去容器内的 `127.0.0.1:9876` 找 namesrv，但 namesrv 运行在**另一个独立容器**中。broker 容器内没有 namesrv 进程，导致 broker 根本无法向 namesrv 注册。

虽然 `broker.conf` 中写了 `namesrvAddr=namesrv:9876`（Docker 服务名），但命令行参数 `-n` 会把 conf 中的值覆盖掉。

### 2. namesrv 监听地址不对外

```yaml
# 错误配置
command: sh mqnamesrv -n 127.0.0.1:9876
```

`-n 127.0.0.1:9876` 使 namesrv **只监听容器内的 lo 接口**。Docker 的端口映射（`9876:9876`）无法将流量转发到绑定在 `127.0.0.1` 的端口上，导致宿主机 Java 应用通过 `localhost:9876` 无法访问 namesrv。

> 注：这个问题被第一个问题掩盖——broker 连不上 namesrv，即使 namesrv 监听正常也没用。但在修复后 dashboad 仍报 `connect to 127.0.0.1:10909 failed`，说明 `brokerIP1=127.0.0.1` 对容器间通信也是问题。

## 修复

### docker-compose.yml

```yaml
# namesrv：去掉 -n 参数，默认监听 0.0.0.0:9876（所有接口）
command: sh mqnamesrv

# broker：使用 Docker 服务名连接 namesrv 容器
command: sh mqbroker -n namesrv:9876 -c /home/rocketmq/rocketmq-5.2.0/conf/broker.conf
```

### broker.conf

```properties
# 改为宿主机 IP，使宿主机 Java 应用和 Docker 容器内 dashboard 均可访问
# 127.0.0.1 只对宿主机有效，dashboard 容器内 127.0.0.1 指向自己
brokerIP1=192.168.52.1
```

### 重启后手动创建 topic

事务消息的 topic 可能不会自动创建，建议通过 mqadmin 手动创建：

```bash
docker exec rmqbroker sh mqadmin updateTopic -n namesrv:9876 -t order-close-topic -c DefaultCluster
```

## 关键点

| 问题 | 原因 | 修复 |
|---|---|---|
| broker 未注册 | `-n 127.0.0.1:9876` 指向容器自身 | 改为 `-n namesrv:9876` |
| namesrv 不响应 | `-n 127.0.0.1:9876` 只监听 lo | 去掉 `-n` 参数 |
| dashboard 连不上 broker | `brokerIP1=127.0.0.1` 对容器不可达 | 改为宿主机 Hyper-V 网卡 IP |
| 事务消息 topic 未创建 | `autoCreateTopicEnable` 对事务消息不生效 | 手动 `mqadmin updateTopic` |