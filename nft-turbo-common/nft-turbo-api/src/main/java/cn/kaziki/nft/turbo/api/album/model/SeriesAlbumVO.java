package cn.kaziki.nft.turbo.api.album.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 单系列图鉴详情
 */
@Getter
@Setter
@ToString
public class SeriesAlbumVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long seriesId;
    private String seriesName;
    private String seriesCover;
    private Integer total;
    private Integer collected;
    private String state;
    private Date completedAt;
    /** 逐卡点亮信息 */
    private List<AlbumCardVO> cards;
    /** 本系列里程碑及完成/领取状态 */
    private List<AlbumMilestoneVO> milestones;
}