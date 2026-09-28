package cn.kaziki.nft.turbo.api.pay.response;

import cn.kaziki.nft.turbo.api.pay.model.PayOrderVO;
import cn.kaziki.nft.turbo.base.response.BaseResponse;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author Hollis
 */
@Getter
@Setter
public class PayQueryResponse extends BaseResponse {

    private List<PayOrderVO> payOrders;
}
