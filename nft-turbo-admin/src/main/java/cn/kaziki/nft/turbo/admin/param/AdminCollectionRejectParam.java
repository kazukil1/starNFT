package cn.kaziki.nft.turbo.admin.param;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 藏品审核驳回参数
 */
@Setter
@Getter
public class AdminCollectionRejectParam {

    @NotNull(message = "藏品ID不能为空")
    private Long collectionId;

    /** 驳回原因 */
    private String rejectReason;
}
