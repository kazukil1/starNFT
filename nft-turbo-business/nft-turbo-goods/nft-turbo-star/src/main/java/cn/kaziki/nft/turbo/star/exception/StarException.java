package cn.kaziki.nft.turbo.star.exception;

import cn.kaziki.nft.turbo.base.exception.BizException;
import cn.kaziki.nft.turbo.base.exception.ErrorCode;

/**
 * 星尘异常
 */
public class StarException extends BizException {

    public StarException(ErrorCode errorCode) {
        super(errorCode);
    }

    public StarException(String message, ErrorCode errorCode) {
        super(message, errorCode);
    }
}
