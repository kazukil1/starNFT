package cn.kaziki.nft.turbo.api.collection.request;

import cn.kaziki.nft.turbo.api.collection.constant.CollectionObtainType;
import cn.kaziki.nft.turbo.api.collection.constant.CollectionRarity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 持有藏品创建请求（API 层 DTO，供跨模块 Dubbo 调用）
 */
@Setter
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class HeldCollectionCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 幂等号 */
    private String identifier;

    /** 藏品名称 */
    private String name;

    /** 藏品封面 */
    private String cover;

    /** 购入价格 */
    private BigDecimal purchasePrice;

    /** 商品ID */
    private Long goodsId;

    /** 稀有度 */
    private CollectionRarity rarity;

    /** 铸造值 */
    private Long forgeValue;

    /** 商品类型 */
    private String goodsType;

    /** 持有人ID */
    private String userId;

    /** 序列号baseId */
    private String serialNoBaseId;

    /** 业务ID */
    private String bizNo;

    /** 业务类型 */
    private String bizType;

    /** 获取途径 */
    private CollectionObtainType obtainType;
}
