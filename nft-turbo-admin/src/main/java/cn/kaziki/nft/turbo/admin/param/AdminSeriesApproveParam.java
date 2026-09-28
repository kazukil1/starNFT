package cn.kaziki.nft.turbo.admin.param;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 系列审核通过参数
 */
@Setter
@Getter
public class AdminSeriesApproveParam {

    @NotNull(message = "系列ID不能为空")
    private Long seriesId;
}
