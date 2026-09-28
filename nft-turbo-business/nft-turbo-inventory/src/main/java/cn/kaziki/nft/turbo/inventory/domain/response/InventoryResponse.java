package cn.kaziki.nft.turbo.inventory.domain.response;

import cn.kaziki.nft.turbo.api.goods.constant.GoodsType;
import cn.kaziki.nft.turbo.base.response.BaseResponse;
import lombok.Getter;
import lombok.Setter;

/**
 * 库存 响应
 */
@Getter
@Setter
public class InventoryResponse extends BaseResponse {

    private String goodsId;

    private GoodsType goodsType;

    private String identifier;

    private Long inventory;
}
