package cn.kaziki.nft.turbo.api.star.request;

import cn.kaziki.nft.turbo.api.star.constant.StarChangeType;
import cn.kaziki.nft.turbo.base.request.BaseRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 星尘变更请求（内部调用）
 */
@Getter
@Setter
@ToString
public class StarChangeRequest extends BaseRequest {

    private static final long serialVersionUID = 1L;

    /** 用户ID */
    @NotNull(message = "userId不能为空")
    private String userId;

    /** 变更数量（正为获得，负为消耗） */
    @NotNull(message = "amount不能为空")
    private Long amount;

    /** 变更类型 */
    @NotNull(message = "changeType不能为空")
    private StarChangeType changeType;

    /** 业务单据号 */
    private String bizNo;

    /** 业务类型 */
    private String bizType;

    /** 幂等号 */
    @NotNull(message = "identifier不能为空")
    private String identifier;
}
