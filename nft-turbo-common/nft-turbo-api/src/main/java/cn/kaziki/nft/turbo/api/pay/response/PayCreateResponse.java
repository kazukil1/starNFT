package cn.kaziki.nft.turbo.api.pay.response;

import cn.kaziki.nft.turbo.base.response.BaseResponse;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Hollis
 */
@Getter
@Setter
public class PayCreateResponse extends BaseResponse {

    private String payOrderId;

    private String payUrl;
}
