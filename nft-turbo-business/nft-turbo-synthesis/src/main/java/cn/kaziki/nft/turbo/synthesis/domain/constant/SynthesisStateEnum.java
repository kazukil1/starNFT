package cn.kaziki.nft.turbo.synthesis.domain.constant;

/**
 * 合成流水状态枚举
 *
 * <pre>
 * SUBMITTED → LOCKED → BURNING → BURNED → MINTING → MINTED
 *                  ↘ FAILED（任意中间阶段可失败回退）
 * </pre>
 */
public enum SynthesisStateEnum {

    /** 已提交，待异步执行 */
    SUBMITTED,

    /** 材料卡已锁定 */
    LOCKED,

    /** 材料销毁中（链上） */
    BURNING,

    /** 材料已销毁，星尘已扣 */
    BURNED,

    /** 产物铸造中（链上） */
    MINTING,

    /** 合成完成 */
    MINTED,

    /** 合成失败 */
    FAILED
}
