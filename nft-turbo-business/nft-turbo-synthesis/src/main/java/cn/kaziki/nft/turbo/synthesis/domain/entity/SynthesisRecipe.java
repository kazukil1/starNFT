package cn.kaziki.nft.turbo.synthesis.domain.entity;

import cn.kaziki.nft.turbo.datasource.domain.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

// 合成配方
@Getter
@Setter
@TableName("synthesis_recipe")
public class SynthesisRecipe extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long seriesId;
    private String name;
    private String sourceRarity;
    private String targetRarity;
    private Integer cardCount;
    private Long starCost;
    private String state;
    private String creatorId;
    private String recipeType;          // RARITY_BASED / CARD_BASED
    private String sourceCollectionIds; // 指定材料藏品ID，逗号分隔（CARD_BASED专用）
    private Long targetCollectionId;    // 指定产物藏品ID（CARD_BASED专用）
}
