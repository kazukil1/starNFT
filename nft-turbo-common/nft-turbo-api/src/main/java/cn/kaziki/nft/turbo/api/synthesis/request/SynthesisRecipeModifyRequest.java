package cn.kaziki.nft.turbo.api.synthesis.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

// 合成配方修改请求
@Getter
@Setter
@ToString
public class SynthesisRecipeModifyRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "配方ID不能为空")
    private Long id;

    // 以下字段均可选更新
    private String name;
    private String sourceRarity;
    private String targetRarity;
    private Integer cardCount;
    private Long starCost;
}
