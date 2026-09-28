package cn.kaziki.nft.turbo.api.star.request;

import cn.kaziki.nft.turbo.api.goods.constant.GoodsEvent;
import cn.kaziki.nft.turbo.base.request.BaseRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Star 写操作请求基类 — 统一携带幂等号 + 事件类型
 */
@Getter
@Setter
public abstract class BaseStarRequest extends BaseRequest {

    private static final long serialVersionUID = 1L;

    /** 幂等号 */
    @NotBlank(message = "幂等号不能为空")
    private String identifier;

    /** 获取对应的事件类型（Inventory 操作返回具体值，纯修改返回 null） */
    public abstract GoodsEvent getEventType();
}
