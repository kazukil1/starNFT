package cn.kaziki.nft.turbo.api.collection.request;

import cn.kaziki.nft.turbo.base.request.PageRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CollectionPageQueryRequest extends PageRequest {

    private String state;

    private String keyword;

    // 创建者ID（可空；Artist 使用时传入自己的 userId）
    private String creatorId;

    // 系列ID（可空；按系列筛选）
    private Long seriesId;

    // 获取途径筛选（可空；LIKE 匹配，如 "DIRECT_SALE"）
    private String obtainType;

    // 稀有度筛选（可空；如 "R"/"SR"/"SSR"/"SP"/"UR"）
    private String rarity;

}
