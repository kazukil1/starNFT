package cn.kaziki.nft.turbo.api.pay.request;

import cn.kaziki.nft.turbo.api.pay.constant.PayChannel;
import cn.kaziki.nft.turbo.base.request.BaseRequest;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * @author Hollis
 */
@Getter
@Setter
public class RefundCreateRequest extends BaseRequest {

    /**
     * 支付单号
     */
    private String payOrderId;

    /**
     * 需要退款的金额
     */
    private BigDecimal refundAmount;

    /**
     * 退款幂等号
     */
    private String identifier;

    /**
     * 退款渠道
     */
    private PayChannel refundChannel;

    /**
     * 退款备注
     */
    private String memo;
}
