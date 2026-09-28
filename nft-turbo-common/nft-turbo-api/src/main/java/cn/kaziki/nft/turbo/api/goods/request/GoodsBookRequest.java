package cn.kaziki.nft.turbo.api.goods.request;

import cn.kaziki.nft.turbo.api.goods.constant.GoodsType;
import cn.kaziki.nft.turbo.api.user.constant.UserType;
import cn.kaziki.nft.turbo.base.request.BaseRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 商品预约 请求
 */
@Getter
@Setter
public class GoodsBookRequest extends BaseRequest {
    // 操作幂等号
    @NotNull(message = "identifier 不能为空")
    private String identifier;

    // 买家id
    @NotNull(message = "买家id不能为空")
    private String buyerId;

    // 买家id类型
    private UserType buyerType = UserType.CUSTOMER;

    // 商品Id
    @NotNull(message = "商品Id不能为空")
    private String goodsId;

    // 商品类型
    private GoodsType goodsType;

}

