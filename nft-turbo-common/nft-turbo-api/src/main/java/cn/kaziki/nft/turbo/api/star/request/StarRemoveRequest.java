package cn.kaziki.nft.turbo.api.star.request;

import cn.kaziki.nft.turbo.api.goods.constant.GoodsEvent;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 星尘闪购包下架请求
@Getter
@Setter
@ToString
public class StarRemoveRequest extends BaseStarRequest {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "闪购包ID不能为空")
    private Long id;

    @Override
    public GoodsEvent getEventType() {
        return GoodsEvent.REMOVE;
    }
}
