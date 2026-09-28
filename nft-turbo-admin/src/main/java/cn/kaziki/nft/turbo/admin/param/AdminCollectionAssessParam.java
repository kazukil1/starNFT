package cn.kaziki.nft.turbo.admin.param;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 藏品评定参数（稀有度/系列/铸造值）
 */
@Setter
@Getter
public class AdminCollectionAssessParam {
    // 藏品id
    @NotNull(message = "藏品id不能为空")
    private Long collectionId;
    // 稀有度：COMMON/RARE/EPIC/LEGENDARY/UNIQUE/MYTHICAL
    @NotBlank(message = "稀有度不能为空")
    private String rarity;
    // 所属系列id（可空；已归入系列后不可更换）
    private Long seriesId;
    // 铸造值（可空，空则按稀有度基准值带出）
    private Long forgeValue;
}
