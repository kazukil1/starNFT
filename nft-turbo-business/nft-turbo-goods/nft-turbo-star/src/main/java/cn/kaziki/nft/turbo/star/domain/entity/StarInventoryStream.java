package cn.kaziki.nft.turbo.star.domain.entity;

import cn.kaziki.nft.turbo.api.goods.constant.GoodsEvent;
import cn.kaziki.nft.turbo.datasource.domain.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 星尘库存流水 — 记录星尘包库存的每一次变动（参照 CollectionInventoryStream）
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("star_inventory_stream")
public class StarInventoryStream extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 流水类型 */
    private GoodsEvent streamType;

    /** 幂等号 */
    private String identifier;

    /** 变更数量 */
    private Long changedQuantity;

    /** 星尘包 ID */
    private Long starId;

    /** 价格 */
    private BigDecimal price;

    /** 发行总量 */
    private Long quantity;

    /** 可售库存 */
    private Long saleableInventory;

    /** 状态 */
    private String state;

    /** 扩展信息 */
    private String extendInfo;

    /** 基于 StarDaily 构建库存流水（参照 CollectionInventoryStream 构造模式） */
    public StarInventoryStream(StarDaily star, String identifier, GoodsEvent streamType, Long changedQuantity) {
        this.starId = star.getId();
        this.price = star.getPrice();
        this.quantity = star.getQuantity();
        this.saleableInventory = star.getSaleableInventory();
        this.state = star.getState().name();
        this.streamType = streamType;
        this.identifier = identifier;
        this.changedQuantity = changedQuantity;
    }
}
