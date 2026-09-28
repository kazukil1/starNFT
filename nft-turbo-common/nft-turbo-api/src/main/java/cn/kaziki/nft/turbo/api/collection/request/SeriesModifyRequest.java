package cn.kaziki.nft.turbo.api.collection.request;

import cn.kaziki.nft.turbo.api.collection.constant.SeriesStateEnum;
import cn.kaziki.nft.turbo.base.request.BaseRequest;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 修改系列 请求（含状态流转）
 */
@Setter
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class SeriesModifyRequest extends BaseRequest {

    // 系列id
    @NotNull(message = "系列id不能为空")
    private Long seriesId;

    // 系列名称
    private String name;

    // 系列封面
    private String cover;

    // 系列故事/介绍
    private String description;

    // 状态
    private SeriesStateEnum state;
}
