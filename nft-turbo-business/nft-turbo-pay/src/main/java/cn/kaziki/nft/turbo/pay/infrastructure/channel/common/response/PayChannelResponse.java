package cn.kaziki.nft.turbo.pay.infrastructure.channel.common.response;

import cn.kaziki.nft.turbo.base.response.BaseResponse;
import lombok.Getter;
import lombok.Setter;

/**
 *
 */
@Setter
@Getter
public class PayChannelResponse extends BaseResponse {
    protected String payUrl;
}
