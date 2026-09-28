package cn.kaziki.nft.turbo.star.domain.entity;

import cn.kaziki.nft.turbo.api.star.constant.StarState;
import cn.kaziki.nft.turbo.api.star.request.StarCreateRequest;
import cn.kaziki.nft.turbo.datasource.domain.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

// 星尘每日闪购配置（表名 star_daily_flash）
@Getter
@Setter
@TableName("star_daily_flash")
public class StarDaily extends BaseEntity {

    private static final long serialVersionUID = 1L;

    // 星尘包名称
    private String name;
    // 封面图片
    private String cover;
    // 星尘数量
    private Long starAmount;
    // 价格
    private BigDecimal price;
    // 数量
    private Long quantity;
    // 详情
    private String detail;
    // 可售库存
    private Long saleableInventory;
    // 创建者id
    private String creatorId;
    // 状态
    private StarState state;
    // 每日开售时间
    private Date saleTime;

    public static StarDaily create(StarCreateRequest request) {
        StarDaily star = new StarDaily();
        star.setName(request.getName());
        star.setCover(request.getCover());
        star.setStarAmount(request.getStarAmount());
        star.setPrice(request.getPrice());
        star.setQuantity(request.getQuantity());
        star.setDetail(request.getDetail());
        star.setSaleTime(request.getSaleTime());
        star.setState(StarState.ACTIVE);
        star.setSaleableInventory(request.getQuantity());
        star.setCreatorId("ADMIN");
        return star;
    }

    // 下架
    public StarDaily remove() {
        this.state = StarState.INACTIVE;
        return this;
    }
}
