package cn.kaziki.nft.turbo.synthesis.exception;

import cn.kaziki.nft.turbo.base.exception.ErrorCode;

// 合成系统错误码
public enum SynthesisErrorCode implements ErrorCode {

    RECIPE_NOT_FOUND("SYNTHESIS_RECIPE_NOT_FOUND", "配方不存在"),
    RECIPE_DISABLED("SYNTHESIS_RECIPE_DISABLED", "配方已停用"),
    CARD_COUNT_MISMATCH("SYNTHESIS_CARD_COUNT_MISMATCH", "材料卡数量与配方不符"),
    MATERIAL_NOT_OWNED("SYNTHESIS_MATERIAL_NOT_OWNED", "材料卡不属于当前用户"),
    MATERIAL_NOT_ACTIVED("SYNTHESIS_MATERIAL_NOT_ACTIVED", "材料卡状态不可合成"),
    MATERIAL_CROSS_SERIES("SYNTHESIS_MATERIAL_CROSS_SERIES", "材料卡跨系列，不支持"),
    MATERIAL_RARITY_MISMATCH("SYNTHESIS_MATERIAL_RARITY_MISMATCH", "材料卡稀有度与配方不符"),
    FORGE_VALUE_NOT_INCREASING("SYNTHESIS_FORGE_VALUE_NOT_INCREASING", "铸造值未增值，合成无效"),
    STAR_BALANCE_NOT_ENOUGH("SYNTHESIS_STAR_NOT_ENOUGH", "星尘余额不足"),
    TARGET_SOLD_OUT("SYNTHESIS_TARGET_SOLD_OUT", "产物藏品已售罄/绝版"),
    DUPLICATE_SUBMIT("SYNTHESIS_DUPLICATE_SUBMIT", "重复提交（幂等）"),
    MATERIAL_NOT_IN_RECIPE("SYNTHESIS_MATERIAL_NOT_IN_RECIPE", "材料卡不在配方指定范围内"),
    STREAM_NOT_FOUND("SYNTHESIS_STREAM_NOT_FOUND", "合成流水不存在"),
    STREAM_SAVE_FAILED("SYNTHESIS_STREAM_SAVE_FAILED", "合成流水保存失败");

    private final String code;
    private final String message;

    SynthesisErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getCode() { return code; }

    @Override
    public String getMessage() { return message; }
}
