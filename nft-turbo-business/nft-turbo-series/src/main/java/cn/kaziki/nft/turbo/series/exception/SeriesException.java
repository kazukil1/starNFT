package cn.kaziki.nft.turbo.series.exception;

/**
 * 系列业务异常
 */
public class SeriesException extends RuntimeException {
    private final String errorCode;

    public SeriesException(SeriesErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode.getCode();
    }

    public String getErrorCode() {
        return errorCode;
    }
}
