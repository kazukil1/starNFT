package cn.kaziki.nft.turbo.synthesis.domain.entity;

import cn.kaziki.nft.turbo.datasource.domain.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

// 合成流水
@Getter
@Setter
@TableName("synthesis_stream")
public class SynthesisStream extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String identifier;
    private String userId;
    private Long recipeId;
    private Long seriesId;
    private String sourceRarity;
    private String targetRarity;
    private Integer cardCount;
    private Long starCost;
    private Long forgeValueBefore;
    private Long forgeValueAfter;
    private String state;
    private Long productHeldId;
    private String materialIds;


}
