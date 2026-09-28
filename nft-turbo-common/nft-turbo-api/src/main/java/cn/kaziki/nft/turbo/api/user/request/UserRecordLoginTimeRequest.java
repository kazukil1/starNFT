package cn.kaziki.nft.turbo.api.user.request;

import cn.kaziki.nft.turbo.base.request.BaseRequest;
import lombok.*;

/**
 * 记录用户登录时间 请求
 */
@Setter
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class UserRecordLoginTimeRequest extends BaseRequest {

    private Long userId;

}