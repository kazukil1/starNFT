package cn.kaziki.nft.turbo.api.box.request;

import cn.kaziki.nft.turbo.api.collection.constant.CollectionRarity;
import cn.kaziki.nft.turbo.base.request.BaseRequest;
import lombok.*;

import java.math.BigDecimal;

/**
 *
 */
@Setter
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class BlindBoxItemCreateRequest extends BaseRequest {

    // 藏品名称
    private String collectionName;

    // 藏品名称
    private String collectionCover;

    // 藏品详情
    private String collectionDetail;

    // 藏品详情
    private BigDecimal referencePrice;

    // 稀有度
    private CollectionRarity rarity;

    // 数量
    private Long quantity;

    // 关联藏品ID（NULL=星尘包）
    private Long collectionId;

    // 星尘数量（NULL=藏品卡，>0=星尘包）
    private Long starAmount;


}
