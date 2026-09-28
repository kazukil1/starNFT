package cn.kaziki.nft.turbo.api.user.request;

import cn.kaziki.nft.turbo.base.request.BaseRequest;
import lombok.*;

/**
 * 用户注册 请求
 */
@Setter
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class UserRegisterRequest extends BaseRequest {

    private String telephone;

    private String inviteCode;

    private String password;

}
