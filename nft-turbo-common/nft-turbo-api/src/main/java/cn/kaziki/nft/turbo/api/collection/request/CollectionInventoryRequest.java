package cn.kaziki.nft.turbo.api.collection.request;

import cn.kaziki.nft.turbo.base.request.BaseRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CollectionInventoryRequest extends BaseRequest {
    @NotNull(message = "collectionId is not null")
    private String collectionId;
    private String identifier;
    private Long inventory;
}
