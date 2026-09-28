package cn.kaziki.nft.turbo.api.order.request;

import cn.kaziki.nft.turbo.api.order.constant.TradeOrderEvent;
import cn.kaziki.nft.turbo.api.user.constant.UserType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;


@Getter
@Setter
public class OrderCreateAndConfirmRequest extends OrderCreateRequest {

    //  操作时间
    @NotNull(message = "operateTime 不能为空")
    private Date operateTime;

    // 操作人
    @NotNull(message = "operator 不能为空")
    private String operator;

    // 操作人类型
    @NotNull(message = "operatorType 不能为空")
    private UserType operatorType;

    // 是否同步扣减库存
    private boolean syncDecreaseInventory = false;

    @Override
    public TradeOrderEvent getOrderEvent() {
        return TradeOrderEvent.CREATE_AND_CONFIRM;
    }
}
