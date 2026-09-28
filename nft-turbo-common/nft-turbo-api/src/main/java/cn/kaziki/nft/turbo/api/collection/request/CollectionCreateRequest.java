package cn.kaziki.nft.turbo.api.collection.request;

import java.math.BigDecimal;
import java.util.Date;

import cn.kaziki.nft.turbo.api.collection.constant.CollectionRarity;
import cn.kaziki.nft.turbo.api.goods.constant.GoodsEvent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 创建藏品 请求
 */

@Setter
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class CollectionCreateRequest extends BaseCollectionRequest {
    // 藏品名称
    private String name;
    // 藏品封面
    private String cover;
    // 藏品详情
    private String detail;
    // 价格
    private BigDecimal price;
    // 藏品数量
    private Long quantity;
    // 稀有度
    @NotNull(message="稀有度不能为空")
    private CollectionRarity rarity;
    // 系列id
    private Long seriesId;
    // 铸造值
    private Long forgeValue;
    // 藏品创建时间
    private Date createTime;
    // 藏品发售时间
    private Date saleTime;
    // 藏品创建者id
    private String creatorId;
    // 可获取途径（逗号分隔）：DIRECT_SALE,BLIND_BOX,SYNTHESIS
    private String obtainType;

    // 藏品是否预约
    @NotNull(message = "藏品是否预约不能为空")
    private Integer canBook;
    // 藏品预约开始时间
    private Date bookStartTime;
    // 藏品预约结束时间
    private Date bookEndTime;

    @Override
    public GoodsEvent getEventType() {
        return GoodsEvent.CREATE;
    }
}
