package cn.kaziki.nft.turbo.api.album.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户全系列图鉴进度概览
 */
@Getter
@Setter
@ToString
public class UserAlbumVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long seriesId;
    private String seriesName;
    private String seriesCover;
    /** 系列内藏品总数 */
    private Integer total;
    /** 已点亮的藏品数 */
    private Integer collected;
    /** COLLECTING / COMPLETED */
    private String state;
    private Date completedAt;
}