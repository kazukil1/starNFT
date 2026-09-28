package cn.kaziki.nft.turbo.collection.domain.entity;

import cn.kaziki.nft.turbo.api.goods.constant.GoodsEvent;
import cn.kaziki.nft.turbo.datasource.domain.entity.BaseEntity;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 藏品空投流水信息
 */
@Getter
@Setter
@NoArgsConstructor
public class CollectionAirdropStream extends BaseEntity {

    // 藏品id
    private Long collectionId;

    // 接收用户ID
    private String recipientUserId;

    // 空投数量
    private Long quantity;

    // 流水类型
    private GoodsEvent streamType;

    // 幂等号
    private String identifier;

    public CollectionAirdropStream(Collection collection, String identifier, GoodsEvent streamType, @Min(value = 1, message = "数量不能小于1") Long quantity, String recipientUserId) {
        this.collectionId = collection.getId();
        this.quantity = quantity;
        this.streamType = streamType;
        this.identifier = identifier;
        this.recipientUserId = recipientUserId;
        super.setLockVersion(collection.getLockVersion());
        super.setDeleted(collection.getDeleted());
    }

}
