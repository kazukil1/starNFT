# InventoryDecreaseTransactionListener 断点不命中

## 现象

运行 `NfTurboBusinessApplication`（nft-turbo-app 模块），调用 `POST /trade/newBuy` 接口后，`InventoryDecreaseTransactionListener.executeLocalTransaction()` 未被触发，断点不命中。

## 根因

`nft-turbo-app` 模块的 `application.yml` 中 `spring.cloud.function.definition` **覆盖**了 `nft-turbo-trade` 模块的定义，导致 `newBuy-out-0` 生产者绑定的 RocketMQ 事务消息配置未生效。

### 调用链路

```
POST /trade/newBuy
  → TradeController.newBuy()
    → StreamProducer.send("newBuy-out-0", tag, msg)
      → StreamBridge.send() 发事务消息
        → RocketMQ 回调 InventoryDecreaseTransactionListener.executeLocalTransaction()
```

### 覆盖关系

| 配置项 | nft-turbo-trade（被覆盖） | nft-turbo-app（生效） |
|--------|--------------------------|---------------------|
| `function.definition` | `newBuy;newBuyPlus;...` | `chain;orderClose;heldCollection` |
| `stream.rocketmq.bindings.newBuy-out-0` | ✅ `producerType: Trans` + `transactionListener: inventoryDecreaseTransactionListener` | ❌ 未配置 |

Spring Boot 多模块加载时，主应用的 `application.yml` 中同名字符串属性 `spring.cloud.function.definition` 会覆盖依赖模块的值，导致 `newBuy` 未注册。

`newBuy-out-0` 的事务生产者配置（`producerType: Trans`、`transactionListener`）虽然在 trade 模块的 yml 中有定义，但由于依赖模块的 yml 加载优先级低于主应用，实际未被 Spring Cloud Stream 识别。`StreamBridge.send("newBuy-out-0", ...)` 发送的是**普通消息**而非事务消息，`TransactionListener` 永远不会被触发。

## 修复

### nft-turbo-app/src/main/resources/application.yml

**1. `function.definition` 精简为仅保留有实际 Consumer bean 的函数：**

```yaml
# 修改前
function:
  definition: chain;orderClose;heldCollection

# 修改后
function:
  definition: orderClose
```

说明：
- `orderClose` 有 `TradeOrderListener.orderClose()` Consumer bean
- `chain`、`heldCollection` 没有对应 bean，之前导致 `Dispatcher has no subscribers` 报错
- `newBuy`、`newBuyPlus` 等是 producer-only 函数（仅 `-out-0` 无 `-in-0`），通过 `StreamBridge` 动态创建绑定，无需在 `function.definition` 中声明

**2. 新增 `newBuy-out-0` 的 stream bindings 和 rocketmq 配置：**

```yaml
spring:
  cloud:
    stream:
      rocketmq:
        bindings:
          newBuy-out-0:
            producer:
              producerType: Trans
              transactionListener: inventoryDecreaseTransactionListener
      bindings:
        newBuy-out-0:
          content-type: application/json
          destination: new-buy-topic
          group: trade-group
          binder: rocketmq
```

## 关键点

| 问题 | 原因 | 修复 |
|------|------|------|
| `function.definition` 被覆盖 | 主应用 yml 优先级高于依赖模块 | 将必要配置直接写在主应用 yml 中 |
| 事务监听器不触发 | `newBuy-out-0` 的 `producerType: Trans` 配置未被加载 | 将 `rocketmq.bindings.newBuy-out-0` 和 `bindings.newBuy-out-0` 补到主应用 yml |
| `chain-in-0` no subscribers | `chain` function 无对应 Consumer bean | 从 `function.definition` 中移除 `chain` |

## 同类风险

| 配置项 | transactionListener | bean 是否存在 |
|--------|-------------------|--------------|
| `orderClose-out-0` | `orderCloseTransactionListener` | ✅ `OrderCloseTransactionListener`（nft-turbo-order），受同样 config merge 影响 |
| `newBuyPlus-out-0` | `orderCreateTransactionListener` | ❌ 不存在 |

建议后续补充 `orderCreateTransactionListener` 实现或移除未使用的绑定配置。

## 验证结果

修复后，以下 TransactionListener 均已正常触发：

| TransactionListener | 绑定 | 触发入口 | 状态 |
|---|---|---|---|
| `InventoryDecreaseTransactionListener` | `newBuy-out-0` | `POST /trade/newBuy` | ✅ |
| `OrderCloseTransactionListener` | `orderClose-out-0` | `POST /trade/cancel` | ✅ |

两个 Listener 的根因相同：主应用 `application.yml` 的 `function.definition` 覆盖了依赖模块的定义，且部分 producer-only 绑定的 rocketmq 事务配置未在主应用 yml 中声明，导致 `StreamBridge.send()` 以普通消息而非事务消息发送。