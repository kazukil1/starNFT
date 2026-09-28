package cn.kaziki.nft.turbo.api.collection.request;

import cn.kaziki.nft.turbo.base.request.BaseRequest;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 创建系列 请求
 */
@Setter
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class SeriesCreateRequest extends BaseRequest {

    // 系列名称
    @NotNull(message = "系列名称不能为空")
    private String name;

    // 系列封面
    private String cover;

    // 系列故事/介绍
    private String description;

    // 创建者id
    private String creatorId;
}
