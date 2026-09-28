package cn.kaziki.nft.turbo.api.user.service;

import cn.kaziki.nft.turbo.api.user.response.UserOperatorResponse;

/**
 * 管理端用户管理 rpc服务
 */
public interface UserManageFacadeService {

    // 用户冻结
    UserOperatorResponse freeze(Long userId);

    // 用户解冻
    UserOperatorResponse unfreeze(Long userId);

}
