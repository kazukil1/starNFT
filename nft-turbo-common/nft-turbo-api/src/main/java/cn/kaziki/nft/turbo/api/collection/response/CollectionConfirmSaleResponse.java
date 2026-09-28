package cn.kaziki.nft.turbo.api.collection.response;

import cn.kaziki.nft.turbo.base.response.BaseResponse;
import lombok.Getter;
import lombok.Setter;

import static cn.kaziki.nft.turbo.base.exception.BizErrorCode.DUPLICATED;

@Getter
@Setter
public class CollectionConfirmSaleResponse extends BaseResponse {
    /**
     * 持有藏品id
     */
    private Long heldCollectionId;

    public static class CollectionSaleBuilder {
        private Long heldCollectionId;

        public CollectionConfirmSaleResponse.CollectionSaleBuilder heldCollectionId(Long heldCollectionId) {
            this.heldCollectionId = heldCollectionId;
            return this;
        }

        public CollectionConfirmSaleResponse buildSuccess() {
            CollectionConfirmSaleResponse goodsSaleResponse = new CollectionConfirmSaleResponse();
            goodsSaleResponse.setHeldCollectionId(heldCollectionId);
            goodsSaleResponse.setSuccess(true);
            return goodsSaleResponse;
        }

        public CollectionConfirmSaleResponse buildDuplicated() {
            CollectionConfirmSaleResponse goodsSaleResponse = new CollectionConfirmSaleResponse();
            goodsSaleResponse.setHeldCollectionId(heldCollectionId);
            goodsSaleResponse.setSuccess(true);
            goodsSaleResponse.setResponseCode(DUPLICATED.getCode());
            goodsSaleResponse.setResponseMessage(DUPLICATED.getMessage());
            return goodsSaleResponse;
        }

        public CollectionConfirmSaleResponse buildFail(String code, String msg) {
            CollectionConfirmSaleResponse goodsSaleResponse = new CollectionConfirmSaleResponse();
            goodsSaleResponse.setHeldCollectionId(heldCollectionId);
            goodsSaleResponse.setSuccess(false);
            goodsSaleResponse.setResponseCode(code);
            goodsSaleResponse.setResponseMessage(msg);
            return goodsSaleResponse;
        }
    }
}