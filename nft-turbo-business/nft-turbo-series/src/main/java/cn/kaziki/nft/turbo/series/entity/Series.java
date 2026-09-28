package cn.kaziki.nft.turbo.series.entity;

import cn.kaziki.nft.turbo.api.collection.constant.SeriesStateEnum;
import cn.kaziki.nft.turbo.api.collection.request.SeriesCreateRequest;
import cn.kaziki.nft.turbo.api.collection.request.SeriesModifyRequest;
import cn.kaziki.nft.turbo.datasource.domain.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 系列 实体
 */
@Getter
@Setter
@TableName("series")
public class Series extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 系列名称 */
    private String name;

    /** 系列封面 */
    private String cover;

    /** 系列故事/介绍 */
    private String description;

    /** 状态 */
    private SeriesStateEnum state;

    /** 创建者id */
    private String creatorId;

    public static Series create(SeriesCreateRequest request) {
        Series series = new Series();
        series.setName(request.getName());
        series.setCover(request.getCover());
        series.setDescription(request.getDescription());
        series.setCreatorId(request.getCreatorId());
        series.setState(SeriesStateEnum.PENDING_REVIEW);
        return series;
    }

    public Series modify(SeriesModifyRequest request) {
        if (request.getName() != null) {
            this.name = request.getName();
        }
        if (request.getCover() != null) {
            this.cover = request.getCover();
        }
        if (request.getDescription() != null) {
            this.description = request.getDescription();
        }
        if (request.getState() != null) {
            this.state = request.getState();
        }
        return this;
    }

    /** 下架 */
    public Series remove() {
        this.state = SeriesStateEnum.REMOVED;
        return this;
    }
}
