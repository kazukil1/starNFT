package cn.kaziki.nft.turbo.api.synthesis.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

// 合成流水 VO
@Getter
@Setter
@ToString
public class SynthesisStreamVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
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
    private String productName;
    private String productCover;
    private String productSerialNo;
    private String productRarity;
    private Date gmtCreate;
}
