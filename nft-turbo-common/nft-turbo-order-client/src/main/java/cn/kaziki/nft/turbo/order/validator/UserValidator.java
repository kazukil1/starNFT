package cn.kaziki.nft.turbo.order.validator;

import cn.kaziki.nft.turbo.api.order.request.OrderCreateRequest;
import cn.kaziki.nft.turbo.api.user.constant.UserRole;
import cn.kaziki.nft.turbo.api.user.constant.UserStateEnum;
import cn.kaziki.nft.turbo.api.user.request.UserQueryRequest;
import cn.kaziki.nft.turbo.api.user.response.UserQueryResponse;
import cn.kaziki.nft.turbo.api.user.response.data.UserInfo;
import cn.kaziki.nft.turbo.api.user.service.UserFacadeService;
import cn.kaziki.nft.turbo.order.OrderException;

import static cn.kaziki.nft.turbo.api.order.constant.OrderErrorCode.*;

/**
 * 用户校验器
 */
public class UserValidator extends BaseOrderCreateValidator {

    private UserFacadeService userFacadeService;

    @Override
    public void doValidate(OrderCreateRequest request) throws OrderException {
        String buyerId = request.getBuyerId();
        UserQueryRequest userQueryRequest = new UserQueryRequest(Long.valueOf(buyerId));
        UserQueryResponse<UserInfo> userQueryResponse = userFacadeService.query(userQueryRequest);
        if (userQueryResponse.getSuccess() && userQueryResponse.getData() != null) {
            UserInfo userInfo = userQueryResponse.getData();
            //判断买家是否是普通用户
            if (userInfo.getUserRole() != null && !userInfo.getUserRole().equals(UserRole.CUSTOMER)) {
                throw new OrderException(BUYER_IS_PLATFORM_USER);
            }
            //判断买家状态
            if (userInfo.getState() != null && !userInfo.getState().equals(UserStateEnum.ACTIVE.name())) {
                throw new OrderException(BUYER_STATUS_ABNORMAL);
            }
        }
    }

    public UserValidator(UserFacadeService userFacadeService) {
        this.userFacadeService = userFacadeService;
    }

    public UserValidator() {
    }
}
