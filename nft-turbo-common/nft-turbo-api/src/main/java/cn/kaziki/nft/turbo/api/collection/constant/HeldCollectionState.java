package cn.kaziki.nft.turbo.api.collection.constant;

/**
 * 持有藏品的状态
 */
public enum HeldCollectionState {
    // 初始化
    INIT,

    // 生效
    ACTIVED,

    // 失效
    INACTIVED,

    // 销毁中
    DESTROYING,

    // 已销毁
    DESTROYED,

    // 合成锁定
    LOCKED;
}
