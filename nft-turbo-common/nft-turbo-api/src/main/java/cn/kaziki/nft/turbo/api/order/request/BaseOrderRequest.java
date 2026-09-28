package cn.kaziki.nft.turbo.api.order.request;

import cn.kaziki.nft.turbo.api.order.constant.TradeOrderEvent;
import cn.kaziki.nft.turbo.base.request.BaseRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public abstract class BaseOrderRequest extends BaseRequest {

    // 操作幂等号
    @NotNull(message = "identifier 不能为空")
    private String identifier;

    // 扩展信息
    private String extendInfo;

    /**
     * 获取订单事件
     *
     * @return
     */
    public abstract TradeOrderEvent getOrderEvent();
}
