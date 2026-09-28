package cn.kaziki.nft.turbo.api.box.request;

import cn.kaziki.nft.turbo.api.goods.constant.GoodsEvent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * 盲盒补充库存 请求
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class BlindBoxModifyInventoryRequest extends BaseBlindBoxRequest {

    /** 补充的条目列表（同创建时的盲盒条目参数） */
    @NotNull(message = "items不能为空")
    private List<BlindBoxItemCreateRequest> items;

    @Override
    public GoodsEvent getEventType() {
        return GoodsEvent.MODIFY_INVENTORY;
    }
}
