package cn.kaziki.nft.turbo.api.collection.request;

import cn.kaziki.nft.turbo.base.request.PageRequest;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class HeldCollectionPageQueryRequest extends PageRequest {

    private String state;

    private String userId;

    private String keyword;
}
