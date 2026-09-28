package cn.kaziki.nft.turbo.gateway.auth;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import cn.kaziki.nft.turbo.api.user.constant.UserPermission;
import cn.kaziki.nft.turbo.api.user.constant.UserRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * sa-token的全局配置
 */
@Configuration
@Slf4j
public class SaTokenConfigure {

    @Bean
    public SaReactorFilter getSaReactorFilter() {
        return new SaReactorFilter()
                // 拦截地址
                .addInclude("/**")
                // 开放地址
                .addExclude("/favicon.ico")
                // 鉴权方法：每次访问进入
                .setAuth(obj -> {
                    // 登录校验 -- 拦截所有路由，并排除/auth/login 用于开放登录
                    SaRouter.match("/**").notMatch("/auth/**", "/artist/login", "/collection/collectionList", "/collection/collectionInfo", "/box/boxList", "/box/boxInfo", "/wxPay/**").check(r -> StpUtil.checkLogin());

                    // 权限认证 -- 不同模块, 校验不同权限
                    SaRouter.match("/admin/**", r -> StpUtil.checkRole(UserRole.ADMIN.name()));
                    SaRouter.match("/artist/**", r -> StpUtil.checkRole(UserRole.ARTIST.name()));
                    SaRouter.match("/trade/**", r -> StpUtil.checkPermission(UserPermission.AUTH.name()));

                    SaRouter.match("/star/**", r -> StpUtil.checkPermission(UserPermission.BASIC.name()));
                    SaRouter.match("/synthesis/**", r -> StpUtil.checkPermission(UserPermission.BASIC.name()));
                    SaRouter.match("/album/**", r -> StpUtil.checkPermission(UserPermission.BASIC.name()));
                    SaRouter.match("/user/**", r -> StpUtil.checkPermissionOr(UserPermission.BASIC.name(), UserPermission.FROZEN.name()));
                    SaRouter.match("/order/**", r -> StpUtil.checkPermissionOr(UserPermission.BASIC.name(),UserPermission.FROZEN.name()));
                })
                // 异常处理方法：每次setAuth函数出现异常时进入
                .setError(this::getSaResult);
    }

    private SaResult getSaResult(Throwable throwable) {
        switch (throwable) {
            case NotLoginException notLoginException:
                log.error("请先登录");
                return SaResult.error("请先登录");
            case NotRoleException notRoleException:
                if (UserRole.ADMIN.name().equals(notRoleException.getRole())) {
                    log.error("请勿越权使用！");
                    return SaResult.error("请勿越权使用！");
                }
                if (UserRole.ARTIST.name().equals(notRoleException.getRole())) {
                    log.error("非创作者用户，无法访问！");
                    return SaResult.error("非创作者用户，无法访问！");
                }
                log.error("您无权限进行此操作！");
                return SaResult.error("您无权限进行此操作！");
            case NotPermissionException notPermissionException:
                if (UserPermission.AUTH.name().equals(notPermissionException.getPermission())) {
                    log.error("请先完成实名认证！");
                    return SaResult.error("请先完成实名认证！");
                }
                log.error("您无权限进行此操作！");
                return SaResult.error("您无权限进行此操作！");
            default:
                return SaResult.error(throwable.getMessage());
        }
    }
}
