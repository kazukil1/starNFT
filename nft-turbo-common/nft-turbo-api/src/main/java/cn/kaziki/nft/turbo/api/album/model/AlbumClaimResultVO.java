package cn.kaziki.nft.turbo.api.album.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * 领取奖励结果
 */
@Getter
@Setter
@ToString
public class AlbumClaimResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Boolean success;
    /** 发放的星尘数 */
    private Long starRewarded;
    /** 空投的持有ID（无空投时为null） */
    private Long airdropHeldId;
}