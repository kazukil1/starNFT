package cn.kaziki.nft.turbo.synthesis.exception;

import cn.kaziki.nft.turbo.base.exception.BizException;
import cn.kaziki.nft.turbo.base.exception.ErrorCode;

// 合成业务异常
public class SynthesisException extends BizException {

    public SynthesisException(ErrorCode errorCode) {
        super(errorCode);
    }

    public SynthesisException(String message, ErrorCode errorCode) {
        super(message, errorCode);
    }
}
