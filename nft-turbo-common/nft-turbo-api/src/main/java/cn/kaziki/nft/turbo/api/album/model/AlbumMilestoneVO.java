package cn.kaziki.nft.turbo.api.album.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * 里程碑配置 + 用户完成/领取状态
 */
@Getter
@Setter
@ToString
public class AlbumMilestoneVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
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
    /** 当前用户是否已完成该里程碑 */
    private Boolean completed;
    /** 当前用户是否已领取 */
    private Boolean claimed;
}