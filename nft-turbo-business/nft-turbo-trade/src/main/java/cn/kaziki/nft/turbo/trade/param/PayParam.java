package cn.kaziki.nft.turbo.trade.param;

import cn.kaziki.nft.turbo.api.pay.constant.PayChannel;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 支付 入参
 */

@Getter
@Setter
public class PayParam {

    @NotNull(message = "orderId is null")
    private String orderId;

    @NotNull(message = "payChannel is null")
    private PayChannel payChannel;

}
