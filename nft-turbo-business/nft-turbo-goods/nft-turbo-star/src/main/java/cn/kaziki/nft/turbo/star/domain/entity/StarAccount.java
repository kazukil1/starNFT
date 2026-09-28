package cn.kaziki.nft.turbo.star.domain.entity;

import cn.kaziki.nft.turbo.datasource.domain.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 星尘账户
 */
@Getter
@Setter
@TableName("star_account")
public class StarAccount extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private String userId;

    /** 当前余额 */
    private Long balance;

    /** 累计获得 */
    private Long totalEarned;

    /** 累计消耗 */
    private Long totalSpent;

    /** 状态：ACTIVE / FROZEN */
    private String state;

    // 初始化新账户
    public static StarAccount init(String userId) {
        StarAccount account = new StarAccount();
        account.setUserId(userId);
        account.setBalance(0L);
        account.setTotalEarned(0L);
        account.setTotalSpent(0L);
        account.setState("ACTIVE");
        return account;
    }
}
