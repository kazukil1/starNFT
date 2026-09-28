package cn.kaziki.nft.turbo.auth.vo;

import cn.dev33.satoken.stp.StpUtil;
import cn.kaziki.nft.turbo.api.user.response.data.UserInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * 登录VO
 */
@Getter
@Setter
@NoArgsConstructor
@ToString
public class LoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    // 用户标识，如用户ID
    private String userId;

    // 访问令牌
    private String token;

    // 令牌过期时间
    private Long tokenExpiration;

    // 最后登录时间
    private Date lastLoginTime;


    public LoginVO(UserInfo userInfo) {
        this.userId = userInfo.getUserId().toString();
        this.token = StpUtil.getTokenValue();
        this.tokenExpiration = StpUtil.getTokenSessionTimeout();
        this.lastLoginTime = userInfo.getLastLoginTime();
    }
}
