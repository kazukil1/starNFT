package cn.kaziki.nft.turbo.api.album.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * 奖励领取记录
 */
@Getter
@Setter
@ToString
public class AlbumRewardRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long seriesId;
    private String milestoneType;
    /** STAR / AIRDROP / BOTH */
    private String rewardType;
    private Long starAmount;
    private Date gmtCreate;
}