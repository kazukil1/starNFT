package cn.kaziki.nft.turbo.api.collection.request;

import cn.kaziki.nft.turbo.api.collection.constant.GoodsSaleBizType;
import cn.kaziki.nft.turbo.api.goods.constant.GoodsEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Setter
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class CollectionSaleRequest extends BaseCollectionRequest {

    /**
     * 藏品名称
     */
    private String name;

    /**
     * 藏品封面
     */
    private String cover;

    /**
     * 藏品类目id
     */
    private String classId;

    /**
     * 购入价格
     */
    private BigDecimal purchasePrice;

    /**
     * '持有人id'
     */
    private String userId;

    /**
     * 销售数量
     */
    private Long quantity;

    /**
     * 业务单号
     */
    private String bizNo;

    /**
     * 业务类型
     * @see GoodsSaleBizType
     */
    private String bizType;


    @Override
    public GoodsEvent getEventType() {
        return GoodsEvent.SALE;
    }
}
