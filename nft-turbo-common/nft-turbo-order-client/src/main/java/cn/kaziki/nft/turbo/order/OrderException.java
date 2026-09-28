package cn.kaziki.nft.turbo.order;

import cn.kaziki.nft.turbo.base.exception.BizException;
import cn.kaziki.nft.turbo.base.exception.ErrorCode;

/**
 *
 */
public class OrderException extends BizException {
    public OrderException(ErrorCode errorCode) {
        super(errorCode);
    }

    public OrderException(String message, ErrorCode errorCode) {
        super(message, errorCode);
    }

    public OrderException(String message, Throwable cause, ErrorCode errorCode) {
        super(message, cause, errorCode);
    }

    public OrderException(Throwable cause, ErrorCode errorCode) {
        super(cause, errorCode);
    }

    public OrderException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, ErrorCode errorCode) {
        super(message, cause, enableSuppression, writableStackTrace, errorCode);
    }
}
