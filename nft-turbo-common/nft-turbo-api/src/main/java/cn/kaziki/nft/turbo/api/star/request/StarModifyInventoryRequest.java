package cn.kaziki.nft.turbo.api.star.request;

import cn.kaziki.nft.turbo.api.goods.constant.GoodsEvent;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 星尘闪购包库存变更请求（正=追加，负=扣减）
@Getter
@Setter
@ToString
public class StarModifyInventoryRequest extends BaseStarRequest {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "闪购包ID不能为空")
    private Long id;

    // 库存变化量（正=追加，负=扣减）
    @NotNull(message = "库存变化量不能为空")
    private Long quantityDelta;

    @Override
    public GoodsEvent getEventType() {
        return GoodsEvent.MODIFY_INVENTORY;
    }
}
