package cn.kaziki.nft.turbo.api.order.response;

import cn.kaziki.nft.turbo.base.response.BaseResponse;
import lombok.Getter;
import lombok.Setter;

import static cn.kaziki.nft.turbo.base.exception.BizErrorCode.DUPLICATED;

/**
 * 订单操作 响应
 */
@Getter
@Setter
public class OrderResponse extends BaseResponse {

    private String orderId;

    private String streamId;

    public static class OrderResponseBuilder {
        private String orderId;
        private String streamId;

        public OrderResponseBuilder orderId(String orderId) {
            this.orderId = orderId;
            return this;
        }

        public OrderResponseBuilder streamId(String streamId) {
            this.streamId = streamId;
            return this;
        }

        public OrderResponse buildSuccess() {
            OrderResponse orderResponse = new OrderResponse();
            orderResponse.setOrderId(orderId);
            orderResponse.setStreamId(streamId);
            orderResponse.setSuccess(true);
            return orderResponse;
        }

        public OrderResponse buildDuplicated() {
            OrderResponse orderResponse = new OrderResponse();
            orderResponse.setOrderId(orderId);
            orderResponse.setStreamId(streamId);
            orderResponse.setSuccess(true);
            orderResponse.setResponseCode(DUPLICATED.getCode());
            orderResponse.setResponseMessage(DUPLICATED.getMessage());
            return orderResponse;
        }

        public OrderResponse buildFail(String code, String msg) {
            OrderResponse orderResponse = new OrderResponse();
            orderResponse.setOrderId(orderId);
            orderResponse.setStreamId(streamId);
            orderResponse.setSuccess(false);
            orderResponse.setResponseCode(code);
            orderResponse.setResponseMessage(msg);
            return orderResponse;
        }
    }
}
