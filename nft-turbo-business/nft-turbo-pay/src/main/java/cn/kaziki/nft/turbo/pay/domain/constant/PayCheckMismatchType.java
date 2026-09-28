package cn.kaziki.nft.turbo.pay.domain.constant;

/**
 * 支付一致性检查结果类型
 *
 * @author Hollis
 */
public enum PayCheckMismatchType {
    /**
     * 支付单不存在
     */
    PAY_ORDER_NOT_EXIST("支付单不存在"),
    /**
     * 渠道侧交易不存在
     */
    CHANNEL_TRANSACTION_NOT_EXIST("渠道侧交易不存在"),
    /**
     * 支付单状态未成功
     */
    PAY_ORDER_STATUS_NOT_SUCCESS("支付单状态未成功"),
    /**
     * 渠道侧交易状态未成功
     */
    CHANNEL_STREAM_STATUS_NOT_SUCCESS("渠道侧支付状态未成功"),
    /**
     * 支付单金额和渠道侧金额不一致
     */
    CHANNEL_STREAM_AMOUNT_NOT_EQUAL_PAY_ORDER_AMOUNT("金额不一致");


    private String desc;

    PayCheckMismatchType(String desc) {
        this.desc = desc;
    }
}
