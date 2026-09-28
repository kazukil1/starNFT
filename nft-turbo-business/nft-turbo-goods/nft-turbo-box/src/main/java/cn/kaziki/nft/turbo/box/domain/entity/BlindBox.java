package cn.kaziki.nft.turbo.box.domain.entity;

import cn.kaziki.nft.turbo.api.box.constant.BlindAllotBoxRule;
import cn.kaziki.nft.turbo.api.box.constant.BlindBoxStateEnum;
import cn.kaziki.nft.turbo.api.box.request.BlindBoxCreateRequest;
import cn.kaziki.nft.turbo.box.domain.entity.convertor.BlindBoxConvertor;
import cn.kaziki.nft.turbo.datasource.domain.entity.BaseEntity;
import com.alibaba.fastjson2.JSON;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 盲盒 实体
 */

@Getter
@Setter
public class BlindBox extends BaseEntity {

    // 盲盒名称
    private String name;

    //盲盒封面
    private String cover;

    // 盲盒详情
    private String detail;

    // 价格
    private BigDecimal price;

    // 盲盒数量
    private Long quantity;

    // 幂等号
    private String identifier;

    // 状态
    private BlindBoxStateEnum state;

    // 可售库存
    private Long saleableInventory;

    // 冻结库存
    private Long frozenInventory;

    // 盲盒创建时间
    private Date createTime;

    // 盲盒发售时间
    private Date saleTime;

    // 上链时间
    private Date syncChainTime;

    // 盲盒分配规则
    private BlindAllotBoxRule allocateRule;

    // 盲盒创建者id
    private String creatorId;

    // 所属系列
    private Long seriesId;

    // 藏品配置
    private String collectionConfigs;

    // 预约开始时间
    private Date bookStartTime;

    // 预约结束时间
    private Date bookEndTime;

    // 是否预约
    private Integer canBook;

    // 概率是否公示（0=隐藏，1=公示，默认1）
    private Integer probabilityPublic;


    public static BlindBox create(BlindBoxCreateRequest request) {
        BlindBox blindBox = BlindBoxConvertor.INSTANCE.mapToEntity(request);
        blindBox.setSaleableInventory(request.getQuantity());
        blindBox.setState(BlindBoxStateEnum.PENDING_REVIEW);
        blindBox.setSeriesId(request.getSeriesId());
        blindBox.setCollectionConfigs(JSON.toJSONString(request.getBlindBoxItemCreateRequests()));
        blindBox.setLockVersion(1);
        return blindBox;
    }
}
