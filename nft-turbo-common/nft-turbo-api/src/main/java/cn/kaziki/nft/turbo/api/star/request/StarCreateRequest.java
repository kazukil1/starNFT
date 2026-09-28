package cn.kaziki.nft.turbo.api.star.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import cn.kaziki.nft.turbo.api.goods.constant.GoodsEvent;

import java.math.BigDecimal;
import java.util.Date;

// 星尘闪购包创建请求（Admin 端使用）
@Getter
@Setter
@ToString
public class StarCreateRequest extends BaseStarRequest {

    private static final long serialVersionUID = 1L;

    // 闪购包名称
    @NotBlank(message = "闪购包名称不能为空")
    private String name;

    // 封面图 URL
    private String cover;

    // 含星尘数量
    @NotNull(message = "星尘数量不能为空")
    private Long starAmount;

    // 售价
    @NotNull(message = "售价不能为空")
    private BigDecimal price;

    // 每日总发行量
    @NotNull(message = "发行量不能为空")
    private Long quantity;

    // 详情描述
    private String detail;

    // 每日开售时间
    @NotNull(message = "开售时间不能为空")
    private Date saleTime;

    @Override
    public GoodsEvent getEventType() {
        return GoodsEvent.CREATE;
    }
}
