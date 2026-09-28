package cn.kaziki.nft.turbo.api.pay.constant;

/**
 * 支付单状态
 */
public enum PayOrderState {

    // 待支付
    TO_PAY,

    // 支付中
    PAYING,

    // 已付款
    PAID,

    // 支付超时
    EXPIRED,

    // 支付失败
    FAILED,

    // 已退款
    REFUNDED;
}
