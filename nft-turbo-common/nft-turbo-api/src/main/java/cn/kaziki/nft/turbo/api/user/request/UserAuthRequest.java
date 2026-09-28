package cn.kaziki.nft.turbo.api.user.request;

import cn.kaziki.nft.turbo.base.request.BaseRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 用户实名认证 请求
 */
@Setter
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class UserAuthRequest extends BaseRequest {

    private Long userId;
    private String realName;
    private String idCard;

}
