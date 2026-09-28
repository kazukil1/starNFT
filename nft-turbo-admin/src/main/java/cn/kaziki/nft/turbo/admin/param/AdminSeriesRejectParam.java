package cn.kaziki.nft.turbo.admin.param;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 系列审核驳回参数
 */
@Setter
@Getter
public class AdminSeriesRejectParam {

    @NotNull(message = "系列ID不能为空")
    private Long seriesId;

    /** 驳回原因 */
    private String rejectReason;
}
