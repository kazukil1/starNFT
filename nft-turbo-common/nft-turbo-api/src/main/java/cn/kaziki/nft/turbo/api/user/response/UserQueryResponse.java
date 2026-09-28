package cn.kaziki.nft.turbo.api.user.response;

import cn.kaziki.nft.turbo.base.response.BaseResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 查询用户 响应
 */
@Setter
@Getter
@ToString
public class UserQueryResponse<T> extends BaseResponse {

    private static final long serialVersionUID = 1L;

    private T data;
}
