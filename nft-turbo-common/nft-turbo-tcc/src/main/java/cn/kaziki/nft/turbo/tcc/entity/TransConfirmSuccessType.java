package cn.kaziki.nft.turbo.tcc.entity;

/**
 * tcc事务confirm成功类型
 */
public enum TransConfirmSuccessType {

    // Confirm成功
    CONFIRM_SUCCESS,

    // 幂等成功
    DUPLICATED_CONFIRM;
}
