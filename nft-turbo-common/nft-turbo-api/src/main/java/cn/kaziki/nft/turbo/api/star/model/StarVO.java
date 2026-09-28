package cn.kaziki.nft.turbo.api.star.model;

import cn.kaziki.nft.turbo.api.goods.model.BaseGoodsVO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 星尘闪购包 VO（商品聚合层返回给 TradeController）
 */
@Getter
@Setter
@ToString
public class StarVO extends BaseGoodsVO {

    private static final long serialVersionUID = 1L;

    // 闪购包主键
    private Long id;

    // 星尘包名称
    private String name;

    // 封面图URL
    private String cover;

    // 含星尘数量
    private Long starAmount;

    // 价格
    private BigDecimal price;

    // 总发行量
    private Long quantity;

    // 详情描述
    private String detail;

    // 可售库存
    private Long remainingInventory;

    // 开售时间
    private Date saleTime;

    // 乐观锁版本
    private Integer version;

    @Override
    public String getGoodsName() {
        return this.name;
    }

    @Override
    public String getGoodsPicUrl() {
        return this.cover;
    }

    @Override
    public String getSellerId() {
        return "";  // 由 Facade 组装时 set
    }

    @Override
    public Integer getVersion() {
        return this.version;
    }

    @Override
    public BigDecimal getPrice() {
        return this.price;
    }

    @Override
    public Boolean canBook() {
        return false;
    }

    @Override
    public Boolean canBookNow() {
        return false;
    }

    @Override
    public Boolean hasBooked() {
        return false;
    }

    @Override
    public Date getBookStartTime() {
        return null;
    }

    @Override
    public Date getBookEndTime() {
        return null;
    }
}
