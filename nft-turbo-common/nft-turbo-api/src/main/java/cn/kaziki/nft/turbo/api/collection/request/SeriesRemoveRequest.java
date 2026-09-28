package cn.kaziki.nft.turbo.api.collection.request;

import cn.kaziki.nft.turbo.base.request.BaseRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 系列下架 请求
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
public class SeriesRemoveRequest extends BaseRequest {

    /** 幂等号 */
    @NotNull(message = "identifier不能为空")
    private String identifier;

    /** 系列ID */
    @NotNull(message = "系列ID不能为空")
    private Long seriesId;
}
