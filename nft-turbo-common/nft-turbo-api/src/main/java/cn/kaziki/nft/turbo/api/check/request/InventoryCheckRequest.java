package cn.kaziki.nft.turbo.api.check.request;

import cn.kaziki.nft.turbo.api.goods.constant.GoodsEvent;
import cn.kaziki.nft.turbo.api.goods.constant.GoodsType;
import cn.kaziki.nft.turbo.base.request.BaseRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 库存检查 请求
 */
@Getter
@Setter
public class InventoryCheckRequest extends BaseRequest {

    // 商品ID
    @NotNull(message = "goodsId不能为空")
    private String goodsId;

    // 商品类型
    @NotNull(message = "goodsType不能为空")
    private GoodsType goodsType;

    // 幂等
    @NotNull(message = "identifier不能为空")
    private String identifier;

    // 变更数量
    @NotNull(message = "changedQuantity不能为空")
    private Long changedQuantity;

    // 商品事件
    @NotNull(message = "goodsEvent不能为空")
    private GoodsEvent goodsEvent;
}
