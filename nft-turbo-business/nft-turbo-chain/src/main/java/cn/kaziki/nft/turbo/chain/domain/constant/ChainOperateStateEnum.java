package cn.kaziki.nft.turbo.chain.domain.constant;

/**
 * 操作 链操作流水 的枚举
 */

public enum ChainOperateStateEnum {
    // 上链成功
    SUCCEED,
    // 上链中
    PROCESSING,
    // 上链失败
    FAILED,
    // 未处理
    INIT

}
