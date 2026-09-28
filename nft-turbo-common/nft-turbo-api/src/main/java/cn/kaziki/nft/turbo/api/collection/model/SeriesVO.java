package cn.kaziki.nft.turbo.api.collection.model;

import cn.kaziki.nft.turbo.api.collection.constant.SeriesStateEnum;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * 藏品系列 VO
 */
@Getter
@Setter
@ToString
public class SeriesVO implements Serializable {

    private static final long serialVersionUID = 1L;

    // 主键ID
    private Long id;

    // 创建者ID
    private String creatorId;

    // 系列名称
    private String name;

    // 系列封面
    private String cover;

    // 系列故事/介绍
    private String description;

    // 状态
    private SeriesStateEnum state;

    // 系列下藏品数量（聚合字段）
    private Long collectionCount;

}
