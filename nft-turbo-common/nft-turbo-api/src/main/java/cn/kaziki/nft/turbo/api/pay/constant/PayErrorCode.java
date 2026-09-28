package cn.kaziki.nft.turbo.api.pay.constant;

import cn.kaziki.nft.turbo.base.exception.ErrorCode;

/**
 * 支付错误码
 */
public enum PayErrorCode implements ErrorCode {
    /**
     * 支付单创建失败
     */
    PAY_ORDER_CREATE_FAILED("PAY_ORDER_CREATE_FAILED", "支付单创建失败"),

    /**
     * 订单已支付过
     */
    ORDER_IS_ALREADY_PAID("ORDER_IS_ALREADY_PAID", "订单已支付过"),

    /**
     * 支付成功回调处理失败
     */
    PAY_SUCCESS_NOTICE_FAILED("PAY_SUCCESS_NOTICE_FAILED", "支付成功回调处理失败"),

    /**
     * 退款创建失败
     */
    REFUND_CREATE_FAILED("REFUND_CREATE_FAILED", "退款创建失败"),

    /**
     * 支付单状态校验失败
     */
    PAY_ORDER_STATUS_CHECK_FAILED("PAY_ORDER_STATUS_CHECK_FAILED", "支付单状态校验失败"),

    /**
     * 退款成功回调处理失败
     */
    REFUND_SUCCESS_NOTICE_FAILED("REFUND_SUCCESS_NOTICE_FAILED", "退款成功回调处理失败");



    private String code;

    private String message;

    PayErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getCode() {
        return this.code;
    }

    @Override
    public String getMessage() {
        return this.message;
    }
}
