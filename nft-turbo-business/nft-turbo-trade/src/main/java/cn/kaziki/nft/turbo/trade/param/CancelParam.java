package cn.kaziki.nft.turbo.trade.param;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 取消订单 入参
 */
@Getter
@Setter
public class CancelParam {

    @NotNull(message = "orderId is null")
    private String orderId;
}
