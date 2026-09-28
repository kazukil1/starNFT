package cn.kaziki.nft.turbo.api.goods.request;

import cn.kaziki.nft.turbo.api.goods.constant.GoodsEvent;
import cn.kaziki.nft.turbo.base.request.BaseRequest;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 通用的商品 请求
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseGoodsRequest extends BaseRequest {

    // 幂等号
    @NotNull(message = "identifier is not null")
    private String identifier;

    // 商品id
    private Long goodsId;

    // 商品类型
    private String goodsType;

    //扩展信息
    private String extendInfo;

    /**
     * 获取事件类型
     *
     * @return
     */
    public abstract GoodsEvent getEventType();
}
