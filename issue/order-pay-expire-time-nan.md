# 订单详情页支付剩余时间显示 NaN:NaN

## 现象

进入订单详情页时，"支付剩余时间"显示 `NaN:NaN` 而非倒计时。

## 根因

后端 `TradeOrderVO.payExpireTime` 类型是 `java.util.Date`，Spring Boot 3.x 默认使用 Jackson 序列化，`java.util.Date` 被输出为 ISO 8601 字符串：

```json
"payExpireTime": "2026-06-27T06:55:49.000+00:00"
```

前端 `getTimeDifference` 方法直接拿这个字符串做减法：

```javascript
const timeLeft = targetTime - now;  // "2026-06-27..." - 1751000000000 = NaN
```

JS 中字符串减数字结果是 `NaN`，后续 `Math.floor(NaN)`、`this.pad(NaN)` 一路污染，最终渲染为 `NaN:NaN`。

另外 `NaN <= 0` 为 `false`，所以已有的 `if (timeLeft <= 0)` 守卫也挡不住。

## 修复

用 `new Date()` 把 `targetTime`（ISO 字符串）转成毫秒时间戳后再计算：

```javascript
// 改前
getTimeDifference(targetTime) {
    const now = new Date().getTime();
    const timeLeft = targetTime - now;  // targetTime 可能是 ISO 字符串
    // ...

// 改后
getTimeDifference(targetTime) {
    const now = new Date().getTime();
    if (!targetTime) return '00:00';
    const target = new Date(targetTime).getTime();  // 兼容字符串和数字
    const timeLeft = target - now;
    if (isNaN(timeLeft) || timeLeft <= 0) return '00:00';
    // ...
}
```

## 关键点

| 项目 | 说明 |
|---|---|
| 后端类型 | `java.util.Date` |
| 序列化框架 | Jackson（Spring Boot 3.x 默认） |
| 序列化格式 | ISO 8601 字符串：`"2026-06-27T06:55:49.000+00:00"` |
| 前端问题 | 直接用字符串减数字，得到 `NaN` |
| 修复方式 | `new Date(targetTime).getTime()` 统一转毫秒 |