package cn.kaziki.nft.turbo.api.collection.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * 铸造值排行榜条目
 */
@Getter
@Setter
@ToString
public class ForgeRankVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 排名（1-based） */
    private Integer rank;

    /** 用户ID */
    private String userId;

    /** 用户昵称 */
    private String nickName;

    /** 用户头像 */
    private String profilePhoto;

    /** 总铸造值 */
    private Long forgeValue;
}
