package cn.kaziki.nft.turbo.admin.vo;

import cn.dev33.satoken.stp.StpUtil;
import cn.kaziki.nft.turbo.api.user.response.data.UserInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class AdminLoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户标识，如用户ID
     */
    private String userId;
    /**
     * 访问令牌
     */
    private String token;

    /**
     * 令牌过期时间
     */
    private Long tokenExpiration;

    /**
     * 最后登录时间
     */
    private Date lastLoginTime;


    public AdminLoginVO(UserInfo userInfo) {
        this.userId = userInfo.getUserId().toString();
        this.token = StpUtil.getTokenValue();
        this.tokenExpiration = StpUtil.getTokenSessionTimeout();
        this.lastLoginTime = userInfo.getLastLoginTime();
    }
}
