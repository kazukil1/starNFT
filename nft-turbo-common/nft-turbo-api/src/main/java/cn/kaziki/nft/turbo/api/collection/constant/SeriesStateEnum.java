package cn.kaziki.nft.turbo.api.collection.constant;

/**
 * 系列状态（与 Collection 一致）
 */
public enum SeriesStateEnum {

    // 草稿
    INIT,

    // 待审核
    PENDING_REVIEW,

    // 已发布（审核通过）
    SUCCEED,

    // 已下架
    REMOVED;
}
