package cn.kaziki.nft.turbo.user.facade;

import cn.kaziki.nft.turbo.api.user.response.UserOperatorResponse;
import cn.kaziki.nft.turbo.api.user.service.UserManageFacadeService;
import cn.kaziki.nft.turbo.user.domain.service.UserService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 管理端用户服务 rpc实现类
 */
@DubboService(version = "1.0.0")
public class UserManageFacadeServiceImpl implements UserManageFacadeService {

    @Autowired
    private UserService userService;

    // 冻结用户
    @Override
    public UserOperatorResponse freeze(Long userId) {
        return userService.freeze(userId);
    }

    // 解冻用户
    @Override
    public UserOperatorResponse unfreeze(Long userId) {
        return userService.unfreeze(userId);
    }
}
