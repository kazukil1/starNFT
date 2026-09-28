package cn.kaziki.nft.turbo.series.exception;

/**
 * 系列错误码
 */
public enum SeriesErrorCode {
    SERIES_NOT_EXIST("SERIES_NOT_EXIST", "系列不存在"),
    SERIES_SAVE_FAILED("SERIES_SAVE_FAILED", "系列保存失败");

    private final String code;
    private final String message;

    SeriesErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
