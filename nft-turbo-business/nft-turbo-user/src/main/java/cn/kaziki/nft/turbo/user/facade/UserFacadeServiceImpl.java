package cn.kaziki.nft.turbo.user.facade;

import cn.kaziki.nft.turbo.api.star.service.StarAccountFacadeService;
import cn.kaziki.nft.turbo.api.user.request.*;
import cn.yueyu.nft.turbo.api.user.request.*;
import cn.kaziki.nft.turbo.api.user.request.condition.UserIdQueryCondition;
import cn.kaziki.nft.turbo.api.user.request.condition.UserPhoneAndPasswordQueryCondition;
import cn.kaziki.nft.turbo.api.user.request.condition.UserPhoneQueryCondition;
import cn.kaziki.nft.turbo.api.user.response.UserOperatorResponse;
import cn.kaziki.nft.turbo.api.user.response.UserQueryResponse;
import cn.kaziki.nft.turbo.api.user.response.data.UserInfo;
import cn.kaziki.nft.turbo.api.user.service.UserFacadeService;
import cn.kaziki.nft.turbo.base.response.PageResponse;
import cn.kaziki.nft.turbo.rpc.facade.Facade;
import cn.kaziki.nft.turbo.user.domain.entity.User;
import cn.kaziki.nft.turbo.user.domain.entity.convertor.UserConvertor;
import cn.kaziki.nft.turbo.user.domain.service.UserService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

/**
 * 客户端用户服务 rpc实现类
 */
@DubboService(version = "1.0.0")
public class UserFacadeServiceImpl implements UserFacadeService {

    @Autowired
    private UserService userService;
    @Autowired
    private StarAccountFacadeService starAccountFacadeService;

    // 根据手机号码获取用户
    @Facade
    @Override
    public UserQueryResponse<UserInfo> query(UserQueryRequest userQueryRequest) {
        User user = switch (userQueryRequest.getUserQueryCondition()) {
            case UserIdQueryCondition userIdQueryCondition:
                yield userService.findById(userIdQueryCondition.getUserId());
            case UserPhoneQueryCondition userPhoneQueryCondition:
                yield userService.findByTelephone(userPhoneQueryCondition.getTelephone());
            case UserPhoneAndPasswordQueryCondition userPhoneAndPasswordQueryCondition:
                yield userService.findByTelephoneAndPass(userPhoneAndPasswordQueryCondition.getTelephone(),
                        userPhoneAndPasswordQueryCondition.getPassword());
            default:
                throw new UnsupportedOperationException(userQueryRequest.getUserQueryCondition() + "''is not supported");
        };
        UserQueryResponse<UserInfo> response = new UserQueryResponse<>();
        response.setSuccess(true);
        response.setData(UserConvertor.INSTANCE.mapToVo(user));
        return response;
    }

    // 分页查询用户信息
    @Override
    public PageResponse<UserInfo> pageQuery(UserPageQueryRequest userPageQueryRequest) {
        var queryResult = userService.pageQueryByState(userPageQueryRequest.getKeyword(), userPageQueryRequest.getState(), userPageQueryRequest.getCurrentPage(), userPageQueryRequest.getPageSize());
        PageResponse<UserInfo> response = new PageResponse<>();
        if (!queryResult.getSuccess()) {
            response.setSuccess(false);
            return response;
        }
        response.setSuccess(true);
        response.setDatas(UserConvertor.INSTANCE.mapToVo(queryResult.getDatas()));
        response.setCurrentPage(queryResult.getCurrentPage());
        response.setPageSize(queryResult.getPageSize());
        return response;
    }

    // 用户注册
    @Override
    @Facade
    public UserOperatorResponse register(UserRegisterRequest userRegisterRequest) {
        UserOperatorResponse response = userService.register(userRegisterRequest.getTelephone(), userRegisterRequest.getInviteCode());
        if (response.getSuccess()) {
            starAccountFacadeService.initAccount(response.getUser().getUserId().toString());
        }
        return response;
    }

    @Override
    @Facade
    public UserOperatorResponse modify(UserModifyRequest userModifyRequest) {
        return null;
    }

    // 实名认证
    @Override
    @Facade
    public UserOperatorResponse auth(UserAuthRequest userAuthRequest) {
        return userService.auth(userAuthRequest);
    }

    @Override
    @Facade
    public UserOperatorResponse active(UserActiveRequest userActiveRequest) {
        return userService.active(userActiveRequest);
    }

    // 记录用户登录时间
    @Override
    @Facade
    public UserQueryResponse<Date> getLoginTime(UserRecordLoginTimeRequest request) {
        Date lastLoginTime = userService.getLoginTime(request.getUserId());
        UserQueryResponse<Date> response = new UserQueryResponse<>();
        response.setData(lastLoginTime);
        response.setSuccess(true);
        return response;
    }
}
