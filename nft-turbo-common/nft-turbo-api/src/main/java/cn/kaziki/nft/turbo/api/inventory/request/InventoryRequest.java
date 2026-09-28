package cn.kaziki.nft.turbo.api.inventory.request;

import cn.kaziki.nft.turbo.api.collection.request.CollectionAirDropRequest;
import cn.kaziki.nft.turbo.api.goods.constant.GoodsType;
import cn.kaziki.nft.turbo.api.order.model.TradeOrderVO;
import cn.kaziki.nft.turbo.api.order.request.OrderCreateRequest;
import cn.kaziki.nft.turbo.base.request.BaseRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 * 统一的库存 请求 -- 创建订单检查库存
 */

@Getter
@Setter
@NoArgsConstructor
public class InventoryRequest extends BaseRequest {

    // 商品ID
    @NotNull(message = "goods is null")
    private String goodsId;

    // 商品类型
    @NotNull(message = "goodsType is null")
    private GoodsType goodsType;

    // 唯一标识
    private String identifier;

    // 库存变化数量
    private Long inventory;

    // 从创建订单参数获取库存请求
    public InventoryRequest(OrderCreateRequest orderCreateRequest) {
        this.goodsId = orderCreateRequest.getGoodsId();
        this.goodsType = orderCreateRequest.getGoodsType();
        this.identifier = orderCreateRequest.getOrderId();
        this.inventory = orderCreateRequest.getItemCount();
    }

    public InventoryRequest(CollectionAirDropRequest request) {
        this.goodsId = request.getCollectionId().toString();
        this.goodsType = GoodsType.COLLECTION;
        this.identifier = request.getIdentifier();
        this.inventory = request.getQuantity();
    }

    public InventoryRequest(TradeOrderVO tradeOrderVO) {
        this.setGoodsId(tradeOrderVO.getGoodsId());
        this.setInventory(tradeOrderVO.getItemCount());
        this.setIdentifier(tradeOrderVO.getOrderId());
        this.setGoodsType(tradeOrderVO.getGoodsType());
    }
}
