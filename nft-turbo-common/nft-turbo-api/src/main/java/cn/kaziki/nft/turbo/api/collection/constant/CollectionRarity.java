package cn.kaziki.nft.turbo.api.collection.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * 藏品稀有度
 */
@AllArgsConstructor
@Getter
public enum CollectionRarity {
    COMMON("N", "普通", 1, 10L),
    RARE("R", "稀有", 2, 30L),
    EPIC("SR", "史诗", 3, 100L),
    LEGENDARY("SSR", "传说", 4, 350L),
    UNIQUE("SP", "独特", 5, 1200L),
    MYTHICAL("UR", "神话", 6, 4000L);

    private final String code;        // 玩法代号，前端主展示
    private final String value;       // 中文名
    private final int level;          // 等级序，比较一律用它
    private final Long baseForgeValue; // 铸造值基准
    // 构造器 + 4 个 getter
}
