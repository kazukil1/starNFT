package cn.kaziki.nft.turbo.gateway.auth;

import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import cn.kaziki.nft.turbo.api.user.constant.UserPermission;
import cn.kaziki.nft.turbo.api.user.constant.UserRole;
import cn.kaziki.nft.turbo.api.user.constant.UserStateEnum;
import cn.kaziki.nft.turbo.api.user.response.data.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 自定义权限验证接口
 */
@Slf4j
@Component
public class StpInterfaceImpl implements StpInterface {
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        UserInfo userInfo = (UserInfo) StpUtil.getSessionByLoginId(loginId).get((String) loginId);

        if (userInfo.getUserRole() == UserRole.ADMIN || userInfo.getState().equals(UserStateEnum.ACTIVE.name()) || userInfo.getState().equals(UserStateEnum.AUTH.name()) ) {
            log.info("当前用户权限：{}，{}",UserPermission.BASIC.name(), UserPermission.AUTH.name());
            return List.of(UserPermission.BASIC.name(), UserPermission.AUTH.name());
        }

        if (userInfo.getState().equals(UserStateEnum.INIT.name())) {
            log.info("当前用户权限：{}",UserPermission.BASIC.name());
            return List.of(UserPermission.BASIC.name());
        }

        if (userInfo.getState().equals(UserStateEnum.FROZEN.name())) {
            log.info("当前用户权限：{}",UserPermission.FROZEN.name());
            return List.of(UserPermission.FROZEN.name());
        }

        log.info("当前用户权限：{}",UserPermission.NONE.name());
        return List.of(UserPermission.NONE.name());
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        UserInfo userInfo = (UserInfo) StpUtil.getSessionByLoginId(loginId).get((String) loginId);
        if (userInfo.getUserRole() == UserRole.ADMIN) {
            return List.of(UserRole.ADMIN.name());
        }
        if (userInfo.getUserRole() == UserRole.ARTIST) {
            return List.of(UserRole.ARTIST.name());
        }
        return List.of(UserRole.CUSTOMER.name());
    }
}
