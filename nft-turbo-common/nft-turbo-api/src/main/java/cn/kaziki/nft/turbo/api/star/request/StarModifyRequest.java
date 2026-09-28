package cn.kaziki.nft.turbo.api.star.request;

import cn.kaziki.nft.turbo.api.goods.constant.GoodsEvent;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Date;

// 星尘闪购包基本信息修改（Admin 端使用）
@Getter
@Setter
@ToString
public class StarModifyRequest extends BaseStarRequest {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "闪购包ID不能为空")
    private Long id;

    // 用户id
    private String userId;

    // 以下字段全可选，传了才更新
    private String name;
    private String cover;
    private Long starAmount;
    private BigDecimal price;
    private String detail;
    private Date saleTime;

    @Override
    public GoodsEvent getEventType() {
        // 纯信息修改，不写库存流水
        return null;
    }
}
