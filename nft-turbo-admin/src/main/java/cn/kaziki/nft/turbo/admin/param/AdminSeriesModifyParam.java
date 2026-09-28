package cn.kaziki.nft.turbo.admin.param;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 系列修改参数（含状态流转）
 */
@Setter
@Getter
public class AdminSeriesModifyParam {

    // 系列id
    @NotNull(message = "系列id不能为空")
    private Long seriesId;

    // 系列名称
    private String name;

    // 系列封面
    private String cover;

    // 系列故事/介绍
    private String description;

    // 状态：INIT/PREVIEW/ON_SALE/SOLD_OUT/FINISHED
    private String state;
}
