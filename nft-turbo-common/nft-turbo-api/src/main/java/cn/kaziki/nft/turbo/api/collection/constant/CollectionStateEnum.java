package cn.kaziki.nft.turbo.api.collection.constant;

/**
 * 未持有的藏品状态
 */
public enum CollectionStateEnum {
    // 未处理（草稿）
    INIT,

    // 待审核
    PENDING_REVIEW,

    // 上链成功
    SUCCEED,

    // 已下架
    REMOVED
}
