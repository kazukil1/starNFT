package cn.kaziki.nft.turbo.admin.param;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Admin 修改星尘包参数
 */
@Setter
@Getter
public class AdminStarModifyParam {

    @NotNull(message = "闪购包ID不能为空")
    private Long id;

    /** 闪购包名称 */
    private String name;

    /** 封面图 URL */
    private String cover;

    /** 详情描述 */
    private String detail;

    /** 售价 */
    private Long price;

    /** 含星尘数量 */
    private Long starAmount;

    /** 开售时间，格式 yyyy-MM-dd HH:mm:ss */
    private String saleTime;
}
