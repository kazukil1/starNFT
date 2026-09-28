package cn.kaziki.nft.turbo.api.user.response;

import cn.kaziki.nft.turbo.api.user.response.data.UserInfo;
import cn.kaziki.nft.turbo.base.response.BaseResponse;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户操作 响应
 */
@Getter
@Setter
public class UserOperatorResponse extends BaseResponse {

    private UserInfo user;
}
