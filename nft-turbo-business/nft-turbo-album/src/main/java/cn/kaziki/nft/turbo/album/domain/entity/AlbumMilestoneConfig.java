package cn.kaziki.nft.turbo.album.domain.entity;

import cn.kaziki.nft.turbo.api.album.request.AlbumMilestoneCreateRequest;
import cn.kaziki.nft.turbo.api.album.request.AlbumMilestoneModifyRequest;
import cn.kaziki.nft.turbo.datasource.domain.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 图鉴里程碑配置
 */
@Getter
@Setter
@TableName("album_milestone_config")
public class AlbumMilestoneConfig extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long seriesId;
    private String name;
    /** ALL_N / ALL_N_R / ALL_SR / ALL_UR */
    private String milestoneType;
    /** 需收集卡数 */
    private Integer requiredCount;
    /** 星尘奖励数量 */
    private Long starReward;
    /** 隐藏款藏品ID（空=无空投） */
    private Long airdropCollectionId;
    /** ACTIVE / DISABLED */
    private String state;

    /** 创建里程碑 */
    public static AlbumMilestoneConfig create(AlbumMilestoneCreateRequest request) {
        AlbumMilestoneConfig config = new AlbumMilestoneConfig();
        config.setSeriesId(request.getSeriesId());
        config.setName(request.getName());
        config.setMilestoneType(request.getMilestoneType());
        config.setRequiredCount(request.getRequiredCount());
        config.setStarReward(request.getStarReward());
        config.setAirdropCollectionId(request.getAirdropCollectionId());
        config.setState("ACTIVE");
        return config;
    }

    /** 修改里程碑 */
    public AlbumMilestoneConfig modify(AlbumMilestoneModifyRequest request) {
        if (request.getName() != null) this.name = request.getName();
        if (request.getStarReward() != null) this.starReward = request.getStarReward();
        if (request.getAirdropCollectionId() != null) this.airdropCollectionId = request.getAirdropCollectionId();
        return this;
    }

    /** 启用 */
    public AlbumMilestoneConfig enable() {
        this.state = "ACTIVE";
        return this;
    }

    /** 停用 */
    public AlbumMilestoneConfig disable() {
        this.state = "DISABLED";
        return this;
    }
}
