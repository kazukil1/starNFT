package cn.kaziki.nft.turbo.tcc.entity;

/**
 * tcc事务try成功类型
 */
public enum TransTrySuccessType {

    // Try成功
    TRY_SUCCESS,

    // 幂等成功
    DUPLICATED_TRY;
}
