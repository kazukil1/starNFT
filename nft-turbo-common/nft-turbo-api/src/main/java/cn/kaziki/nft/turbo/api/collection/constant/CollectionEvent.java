package cn.kaziki.nft.turbo.api.collection.constant;

/**
 * 操作藏品 事件类型
 */
public enum CollectionEvent {

    // 上链事件
    CHAIN,

    // 销毁事件
    DESTROY,

    // 出售事件（出售、预扣减、确认出售、取消出售）
    SALE,
    TRY_SALE,
    CONFIRM_SALE,
    CANCEL_SALE,

    // 转移事件
    TRANSFER,

    // 下架
    REMOVE,

    // 修改藏品库存
    MODIFY_INVENTORY,

    // 修改藏品价格
    MODIFY_PRICE;
}
