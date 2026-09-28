package cn.kaziki.nft.turbo.api.collection.model;

import cn.kaziki.nft.turbo.api.collection.constant.CollectionRarity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 持有藏品信息
 */
@Getter
@Setter
@ToString
public class HeldCollectionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    // id
    private String id;

    // 藏品名称
    private String name;

    // 藏品封面
    private String cover;

    // 购入价格
    private BigDecimal purchasePrice;

    // 稀有度
    private CollectionRarity rarity;

    // 铸造值（创建时从藏品带入的快照）
    private Long forgeValue;

    // 藏品id
    private Long collectionId;

    // 藏品编号
    private String serialNo;

    // nft唯一编号
    private String nftId;

    // 上一个持有人id
    private String preId;

    // 持有人id
    private String userId;

    // 状态
    private String state;

    // 交易hash
    private String txHash;

    // 藏品持有时间
    private Date holdTime;

    // 藏品同步时间
    private Date syncChainTime;

    // 藏品销毁时间
    private Date deleteTime;

    // 业务单号
    private String bizNo;

    // 业务类型
    private String bizType;

    // 获取途径
    private String obtainType;

    // 系列名称（冗余，Facade 手动填充）
    private String seriesName;

}
