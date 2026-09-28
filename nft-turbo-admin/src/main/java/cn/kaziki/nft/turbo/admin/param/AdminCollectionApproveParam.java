package cn.kaziki.nft.turbo.admin.param;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 藏品审核通过参数
 */
@Setter
@Getter
public class AdminCollectionApproveParam {

    @NotNull(message = "藏品ID不能为空")
    private Long collectionId;
}
