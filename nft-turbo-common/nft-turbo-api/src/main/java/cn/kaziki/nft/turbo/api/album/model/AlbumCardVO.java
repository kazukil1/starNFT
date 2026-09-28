package cn.kaziki.nft.turbo.api.album.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * 单卡点亮信息
 */
@Getter
@Setter
@ToString
public class AlbumCardVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long collectionId;
    private String name;
    private String cover;
    private String rarity;
    /** 当前是否持有 */
    private Boolean owned;
    /** 是否曾经持有过 */
    private Boolean everHad;
    /** 首次获得时间 */
    private Date firstObtainTime;
}