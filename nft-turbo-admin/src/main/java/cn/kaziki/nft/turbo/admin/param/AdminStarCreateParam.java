package cn.kaziki.nft.turbo.admin.param;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Admin 创建星尘包参数
 */
@Setter
@Getter
public class AdminStarCreateParam {

    @NotBlank(message = "闪购包名称不能为空")
    private String name;

    /** 封面图 URL */
    private String cover;

    /** 详情描述 */
    private String detail;

    @NotNull(message = "售价不能为空")
    private Long price;

    @NotNull(message = "发行量不能为空")
    private Long quantity;

    @NotNull(message = "星尘数量不能为空")
    private Long starAmount;

    /** 开售时间，格式 yyyy-MM-dd HH:mm:ss */
    @NotBlank(message = "开售时间不能为空")
    private String saleTime;
}
