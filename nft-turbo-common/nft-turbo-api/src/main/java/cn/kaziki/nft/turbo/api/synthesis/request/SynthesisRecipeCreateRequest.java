package cn.kaziki.nft.turbo.api.synthesis.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

// 合成配方创建请求
@Getter
@Setter
@ToString
public class SynthesisRecipeCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "系列ID不能为空")
    private Long seriesId;

    @NotBlank(message = "配方名称不能为空")
    private String name;

    @NotBlank(message = "材料稀有度不能为空")
    private String sourceRarity;

    @NotBlank(message = "产物稀有度不能为空")
    private String targetRarity;

    @NotNull(message = "需烧卡数量不能为空")
    private Integer cardCount;

    @NotNull(message = "星尘消耗不能为空")
    private Long starCost;

    /** 创建者 ID（由 Artist Controller 注入当前用户，Facade 透传至 Service） */
    private String creatorId;

    /** 配方类型，默认 RARITY_BASED（稀有度配方），可选 CARD_BASED（指定卡配方） */
    private String recipeType;

    /** 指定材料藏品ID列表（CARD_BASED 专用） */
    private List<Long> sourceCollectionIds;

    /** 指定产物藏品ID（CARD_BASED 专用） */
    private Long targetCollectionId;
}
