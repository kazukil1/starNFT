package cn.kaziki.nft.turbo.collection.domain.entity;

import cn.kaziki.nft.turbo.api.collection.constant.CollectionObtainType;
import cn.kaziki.nft.turbo.api.collection.constant.CollectionRarity;
import cn.kaziki.nft.turbo.api.collection.constant.CollectionStateEnum;
import cn.kaziki.nft.turbo.api.collection.request.CollectionCreateRequest;
import cn.kaziki.nft.turbo.collection.domain.entity.convertor.CollectionConvertor;
import cn.kaziki.nft.turbo.datasource.domain.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import org.dromara.easyes.annotation.IndexName;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 未持有的藏品 实体
 */
@Getter
@Setter
@Document(indexName = "nfturbo_collection")
@IndexName(value = "nfturbo_collection")
public class Collection extends BaseEntity {

    // 显式固定序列化版本号：实体被 JetCache 以 Java 序列化缓存（valueEncoder: java），
    // 不固定的话新增字段会改变默认 serialVersionUID，导致 Redis 旧缓存反序列化失败
    private static final long serialVersionUID = 1L;
    // 藏品名称
    private String name;
    // 藏品封面
    private String cover;
    // 藏品类目id
    private String classId;
    // 价格
    private BigDecimal price;
    // 藏品数量
    private Long quantity;
    // 藏品详情
    private String detail;
    // 稀有度
    @Field(name = "rarity", type = FieldType.Keyword)
    private CollectionRarity rarity;
    // 所属系列id
    @Field(name = "series_id", type = FieldType.Long)
    private Long seriesId;
    // 铸造值
    @Field(name = "forge_value", type = FieldType.Long)
    private Long forgeValue;
    // 可获取途径（逗号分隔）：DIRECT_SALE,BLIND_BOX,SYNTHESIS
    @Field(name = "obtain_type", type = FieldType.Keyword)
    private String obtainType;
    // 可售库存
    @Field(name = "saleable_inventory", type = FieldType.Long)
    private Long saleableInventory;
    // 已占库存   @deprecated 这个字段不再使用，详见 CollecitonSerivce.confirmSale
    @Deprecated
    private Long occupiedInventory;
    // 被冻结库存
    private Long frozenInventory;
    // 状态
    private CollectionStateEnum state;
    // 藏品创建时间
    @Field(name = "create_time", type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd HH:mm:ss || strict_date_optional_time || epoch_millis")
    private Date createTime;
    // 藏品发售时间
    @Field(name = "sale_time", type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd HH:mm:ss || strict_date_optional_time || epoch_millis")
    private Date saleTime;
    // 藏品上链时间
    @Field(name = "sync_chain_time", type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd HH:mm:ss || strict_date_optional_time || epoch_millis")
    private Date syncChainTime;
    // 藏品创建者id
    private String creatorId;
    // 版本
    private Integer version;
    // 预约开始时间
    @Field(name = "book_start_time", type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd HH:mm:ss || strict_date_optional_time || epoch_millis")
    private Date bookStartTime;
    // 预约结束时间
    @Field(name = "book_end_time", type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd HH:mm:ss || strict_date_optional_time || epoch_millis")
    private Date bookEndTime;
    // 是否预约
    @Field(name = "can_book", type = FieldType.Integer)
    private Integer canBook;


    public static Collection create(CollectionCreateRequest request) {
        Collection collection = CollectionConvertor.INSTANCE.mapToEntity(request);
        collection.setFrozenInventory(0L);
        collection.setSaleableInventory(request.getQuantity());
        collection.setObtainType(
            request.getObtainType() != null && !request.getObtainType().isBlank()
                ? request.getObtainType()
                : CollectionObtainType.DEFAULT_COLLECTION_CHANNELS
        );
        collection.setState(CollectionStateEnum.PENDING_REVIEW);
        collection.setVersion(0);
        // 铸造值未指定时按稀有度基准值带出
        if (collection.getForgeValue() == null && collection.getRarity() != null) {
            collection.setForgeValue(collection.getRarity().getBaseForgeValue());
        }
        return collection;
    }

    public Collection remove() {
        this.state = CollectionStateEnum.REMOVED;
        return this;
    }
}
