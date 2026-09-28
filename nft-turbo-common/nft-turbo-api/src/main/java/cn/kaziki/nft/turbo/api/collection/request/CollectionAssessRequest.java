package cn.kaziki.nft.turbo.api.collection.request;

import cn.kaziki.nft.turbo.api.collection.constant.CollectionRarity;
import cn.kaziki.nft.turbo.api.goods.constant.GoodsEvent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 藏品评定请求（稀有度/系列/铸造值/获取途径）
 * 窄请求模式，与 CollectionModifyPriceRequest 平级；collectionId 复用父类字段
 */
@Setter
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class CollectionAssessRequest extends BaseCollectionRequest {

    // 稀有度
    @NotNull(message = "稀有度不能为空")
    private CollectionRarity rarity;

    // 系列id
    private Long seriesId;

    // 铸造值（空则按稀有度基准值带出）
    private Long forgeValue;

    @Override
    public GoodsEvent getEventType() {
        return GoodsEvent.ASSESS;
    }
}
