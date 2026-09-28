package cn.kaziki.nft.turbo.api.user.request;

import cn.kaziki.nft.turbo.base.request.BaseRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 用户上链 请求
 */
@Setter
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class UserActiveRequest extends BaseRequest {

    private String userId;
    private String blockChainPlatform;
    private String blockChainUrl;

}
