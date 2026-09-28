package cn.kaziki.nft.turbo.album.exception;

import cn.kaziki.nft.turbo.base.exception.BizException;
import cn.kaziki.nft.turbo.base.exception.ErrorCode;

/**
 * 图鉴业务异常
 */
public class AlbumException extends BizException {

    public AlbumException(ErrorCode errorCode) {
        super(errorCode);
    }

    public AlbumException(String message, ErrorCode errorCode) {
        super(message, errorCode);
    }
}