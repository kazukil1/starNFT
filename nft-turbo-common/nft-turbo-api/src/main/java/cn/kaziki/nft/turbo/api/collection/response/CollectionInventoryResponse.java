package cn.kaziki.nft.turbo.api.collection.response;

import cn.kaziki.nft.turbo.base.response.BaseResponse;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CollectionInventoryResponse extends BaseResponse {
    private String collectionId;

    private String identifier;

    private Long Inventory;
}
