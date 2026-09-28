package cn.kaziki.nft.turbo.star.exception;

import cn.kaziki.nft.turbo.base.exception.ErrorCode;

/**
 * 星尘相关错误码
 */
public enum StarErrorCode implements ErrorCode {

    ACCOUNT_SAVE_FAILED("STAR_ACCOUNT_SAVE_FAILED", "星尘账户保存失败"),
    ACCOUNT_QUERY_FAILED("STAR_ACCOUNT_QUERY_FAILED", "星尘账户查询失败"),
    ACCOUNT_NOT_EXIST("STAR_ACCOUNT_NOT_EXIST", "星尘账户未初始化，请先注册"),
    BALANCE_NOT_ENOUGH("STAR_BALANCE_NOT_ENOUGH", "星尘余额不足"),
    STREAM_SAVE_FAILED("STAR_STREAM_SAVE_FAILED", "星尘流水保存失败"),
    STREAM_QUERY_FAILED("STAR_STREAM_QUERY_FAILED", "星尘流水查询失败"),
    DUPLICATE_OPERATION("STAR_DUPLICATE_OPERATION", "星尘操作已执行（幂等）"),
    STAR_NOT_EXIST("STAR_NOT_EXIST", "星尘包不存在"),
    STAR_SOLD_OUT("STAR_SOLD_OUT", "星尘包已售罄"),
    STAR_SALE_FAILED("STAR_SALE_FAILED", "星尘包库存扣减失败"),
    STAR_CANCEL_FAILED("STAR_CANCEL_FAILED", "星尘包库存回滚失败"),
    STAR_STREAM_SAVE_FAILED("STAR_STREAM_SAVE_FAILED", "星尘库存流水保存失败");

    private final String code;
    private final String message;

    StarErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
