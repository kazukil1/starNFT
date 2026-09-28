package cn.kaziki.nft.turbo.api.synthesis.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

// 合成配方 VO
@Getter
@Setter
@ToString
public class SynthesisRecipeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long seriesId;
    private String seriesName;
    private String name;
    private String sourceRarity;
    private String targetRarity;
    private Integer cardCount;
    private Long starCost;
    private String state;
    private String recipeType;          // RARITY_BASED / CARD_BASED
    private String sourceCollectionIds; // 指定材料藏品ID列表（CARD_BASED专用）
    private Long targetCollectionId;    // 指定产物藏品ID（CARD_BASED专用）
}
