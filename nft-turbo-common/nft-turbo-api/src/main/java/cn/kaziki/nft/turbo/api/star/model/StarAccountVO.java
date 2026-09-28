package cn.kaziki.nft.turbo.api.star.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * 星尘账户信息 VO
 */
@Getter
@Setter
@ToString
public class StarAccountVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前余额 */
    private Long balance;

    /** 累计获得 */
    private Long totalEarned;

    /** 累计消耗 */
    private Long totalSpent;

    /** 账户状态：ACTIVE / FROZEN */
    private String state;
}
