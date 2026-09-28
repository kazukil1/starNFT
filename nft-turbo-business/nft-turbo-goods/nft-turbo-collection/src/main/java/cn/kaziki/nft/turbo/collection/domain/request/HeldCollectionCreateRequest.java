package cn.kaziki.nft.turbo.collection.domain.request;

import cn.kaziki.nft.turbo.api.collection.constant.CollectionRarity;
import cn.kaziki.nft.turbo.api.collection.request.CollectionAirDropRequest;
import cn.kaziki.nft.turbo.api.collection.request.CollectionConfirmSaleRequest;
import cn.kaziki.nft.turbo.api.goods.constant.GoodsType;
import cn.kaziki.nft.turbo.collection.domain.constant.HeldCollectionEventType;
import cn.kaziki.nft.turbo.collection.domain.entity.Collection;
import cn.kaziki.nft.turbo.collection.domain.entity.CollectionAirdropStream;
import lombok.*;

import java.math.BigDecimal;

/**
 * 持有藏品创建请求
 */

@Setter
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class HeldCollectionCreateRequest extends BaseHeldCollectionRequest {
    // 藏品名称
    private String name;

    // 藏品封面
    private String cover;

    // 购入价格
    private BigDecimal purchasePrice;

    // 参考价格
    private BigDecimal referencePrice;

    // 商品 id
    private Long goodsId;

    // 稀有度
    private CollectionRarity rarity;

    // 铸造值（创建时从藏品带入的快照）
    private Long forgeValue;

    // 商品类型
    private String goodsType;

    // 持有人id
    private String userId;

    // 藏品编号
    @Deprecated
    private String serialNo;

    /**
     * 序列号生成的 baseId，在商品为藏品时，该 id 为藏品 id，在商品为盲盒时，该 id 为盲盒 id
     */
    private String serialNoBaseId;

    // 业务Id
    private String bizNo;

    // 业务类型
    private String bizType;

    @Deprecated
    public HeldCollectionCreateRequest(CollectionConfirmSaleRequest collectionConfirmSaleRequest, String serialNo) {
        this.goodsId = collectionConfirmSaleRequest.collectionId();
        this.userId = collectionConfirmSaleRequest.userId();
        this.bizNo = collectionConfirmSaleRequest.bizNo();
        this.bizType = collectionConfirmSaleRequest.bizType();
        this.name = collectionConfirmSaleRequest.name();
        this.cover = collectionConfirmSaleRequest.cover();
        this.purchasePrice = collectionConfirmSaleRequest.purchasePrice();
        this.serialNo = serialNo;
    }

    public HeldCollectionCreateRequest(CollectionAirDropRequest airDropRequest, Collection collection, CollectionAirdropStream airdropStream) {
        this.goodsId = airDropRequest.getCollectionId();
        this.userId = airDropRequest.getRecipientUserId();
        this.name = collection.getName();
        this.cover = collection.getCover();
        this.purchasePrice = collection.getPrice();
        this.rarity = collection.getRarity();
        this.forgeValue = collection.getForgeValue();
        this.bizType = airDropRequest.getBizType().name();
        this.bizNo = airdropStream.getId().toString();
        this.serialNoBaseId = String.valueOf(collection.getId());
        this.goodsType = GoodsType.COLLECTION.name();
    }

    @Override
    public HeldCollectionEventType getEventType() {
        return HeldCollectionEventType.CREATE;
    }
}
