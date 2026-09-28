package cn.kaziki.nft.turbo.api.collection.response;

import cn.kaziki.nft.turbo.base.response.BaseResponse;
import lombok.Getter;
import lombok.Setter;

/**
 * 藏品创建响应（不含链信息，上链移至审核通过阶段）
 */
@Getter
@Setter
public class CollectionCreateResponse extends BaseResponse {
    /**
     * 藏品id
     */
    private Long collectionId;

}
