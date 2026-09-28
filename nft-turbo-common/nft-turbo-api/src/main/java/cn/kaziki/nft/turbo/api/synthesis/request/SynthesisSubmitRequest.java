package cn.kaziki.nft.turbo.api.synthesis.request;

import cn.kaziki.nft.turbo.base.request.BaseRequest;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

// 合成提交请求
@Getter
@Setter
@ToString
public class SynthesisSubmitRequest extends BaseRequest {

    // 幂等号
    private String identifier;

    // 用户ID
    private String userId;

    @NotNull(message = "配方ID不能为空")
    private Long recipeId;

    @NotEmpty(message = "材料卡列表不能为空")
    private List<String> materialIds;
}
