package cn.kaziki.nft.turbo.api.user.response.data;

import cn.kaziki.nft.turbo.api.user.constant.UserRole;
import cn.kaziki.nft.turbo.api.user.constant.UserStateEnum;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户信息
 */
@Getter
@Setter
@NoArgsConstructor
public class UserInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    // 用户Id
    private Long userId;

    // 昵称
    private String nickName;

    // 手机号
    private String telephone;

    // 状态
    private String state;

    // 头像地址
    private String profilePhotoUrl;

    // 区块链地址
    private String blockChainUrl;

    // 区块链平台
    private String blockChainPlatform;

    // 实名认证
    private Boolean certification;

    // 用户角色
    private UserRole userRole;

    // 邀请码
    private String inviteCode;

    // 注册时间
    private Date createTime;

    // 最后登录时间
    private Date lastLoginTime;

    public boolean userCanBuy() {

        if (this.getUserRole() != null && !this.getUserRole().equals(UserRole.CUSTOMER)) {
            return false;
        }
        //判断买家状态
        if (this.getState() != null && !this.getState().equals(UserStateEnum.ACTIVE.name())) {
            return false;
        }

        return true;
    }
}
