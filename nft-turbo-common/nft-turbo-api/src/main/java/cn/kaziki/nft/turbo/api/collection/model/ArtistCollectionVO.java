package cn.kaziki.nft.turbo.api.collection.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Artist 端藏品 VO — 比 C 端多持有人数等运营数据
 */
@Getter
@Setter
@ToString(callSuper = true)
public class ArtistCollectionVO extends CollectionVO {

    // 持有者人数
    private Long holderCount;
}
