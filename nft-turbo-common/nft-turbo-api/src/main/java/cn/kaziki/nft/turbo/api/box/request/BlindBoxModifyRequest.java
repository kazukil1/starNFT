package cn.kaziki.nft.turbo.api.box.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 盲盒修改请求
 */
@Getter
@Setter
public class BlindBoxModifyRequest {

    // 盲盒ID
    private Long boxId;

    // 盲盒名称
    private String name;

    // 盲盒封面
    private String cover;

    // 盲盒详情
    private String detail;

    // 价格
    private BigDecimal price;

    // 盲盒数量
    private Long quantity;

    // 发售时间
    private Date saleTime;

    // 所属系列
    private Long seriesId;
}