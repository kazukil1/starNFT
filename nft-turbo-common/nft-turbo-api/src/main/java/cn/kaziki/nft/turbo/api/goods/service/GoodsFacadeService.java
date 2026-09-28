package cn.kaziki.nft.turbo.api.goods.service;

import cn.kaziki.nft.turbo.api.goods.constant.GoodsEvent;
import cn.kaziki.nft.turbo.api.goods.constant.GoodsType;
import cn.kaziki.nft.turbo.api.goods.model.BaseGoodsVO;
import cn.kaziki.nft.turbo.api.goods.model.GoodsStreamVO;
import cn.kaziki.nft.turbo.api.goods.request.GoodsBookRequest;
import cn.kaziki.nft.turbo.api.goods.request.GoodsSaleRequest;
import cn.kaziki.nft.turbo.api.goods.response.GoodsBookResponse;
import cn.kaziki.nft.turbo.api.goods.response.GoodsSaleResponse;
import cn.kaziki.nft.turbo.base.response.BaseResponse;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 商品服务
 */
public interface GoodsFacadeService {
    // 获取商品
    BaseGoodsVO getGoods(String goodsId, GoodsType goodsType);

    // 预约商品
    GoodsBookResponse book(GoodsBookRequest req);

    // 商品是否已被预约
    Boolean isGoodsBooked(@NotNull(message = "商品Id不能为空") String goodsId, GoodsType goodsType, @NotNull(message = "买家id不能为空") String buyerId);

    // 获取商品库存流水
    GoodsStreamVO getGoodsInventoryStream(@NotNull String goodsId, @NotNull GoodsType goodsType, @NotNull GoodsEvent goodsEvent, @NotNull String identifier);

    // 售卖
    BaseResponse sale(GoodsSaleRequest goodsSaleRequest);
    GoodsSaleResponse saleWithoutHint(GoodsSaleRequest goodsSaleRequest);

    // 取消购买
    GoodsSaleResponse cancelSale(GoodsSaleRequest goodsSaleRequest);

    // 支付成功
    GoodsSaleResponse paySuccess(GoodsSaleRequest req);

    // 添加热门商品
    Boolean addHotGoods(String goodsId, String goodsType);

    // 是否是热门商品
    Boolean isHotGoods(String goodsId, String goodsType);


    // 获取热门商品id列表
    List<String> getHotGoods(String goodsType);
}
