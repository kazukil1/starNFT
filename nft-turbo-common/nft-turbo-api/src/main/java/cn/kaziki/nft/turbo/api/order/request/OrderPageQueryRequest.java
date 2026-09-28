package cn.kaziki.nft.turbo.api.order.request;

import cn.kaziki.nft.turbo.base.request.PageRequest;
import lombok.Getter;
import lombok.Setter;

/**
 * 订单分页查询 请求
 */
@Getter
@Setter
public class OrderPageQueryRequest extends PageRequest {

    // 买家id
    private String buyerId;

    // 卖家id
    private String sellerId;

    // 订单id
    private String orderId;

    // 订单状态
    private String state;
}
