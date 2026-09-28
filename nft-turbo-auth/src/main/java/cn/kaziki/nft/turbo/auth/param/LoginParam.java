package cn.kaziki.nft.turbo.auth.param;

import lombok.Getter;
import lombok.Setter;

/**
 * 登录参数
 */
@Setter
@Getter
public class LoginParam extends RegisterParam {

    // 记住账号
    private Boolean rememberMe;
}
