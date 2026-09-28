package cn.kaziki.nft.turbo.api.box.constant;

/**
 * 盲盒状态 枚举
 */
public enum BlindBoxStateEnum {
    // 未处理（草稿）
    INIT,

    // 待审核
    PENDING_REVIEW,

    // 发布成功
    SUCCEED,

    // 已下架
    REMOVED
}
