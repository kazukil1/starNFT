package cn.kaziki.nft.turbo.admin.param;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Hollis
 */
@Setter
@Getter
public class AdminLoginParam extends AdminRegisterParam {

    /**
     * 记住我
     */
    private Boolean rememberMe;
}
