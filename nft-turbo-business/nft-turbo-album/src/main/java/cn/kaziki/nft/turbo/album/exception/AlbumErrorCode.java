package cn.kaziki.nft.turbo.album.exception;

import cn.kaziki.nft.turbo.base.exception.ErrorCode;

/**
 * 图鉴系统错误码
 */
public enum AlbumErrorCode implements ErrorCode {

    MILESTONE_NOT_FOUND("ALBUM_MILESTONE_NOT_FOUND", "里程碑配置不存在"),
    MILESTONE_NOT_COMPLETED("ALBUM_MILESTONE_NOT_COMPLETED", "里程碑条件未达成"),
    ALREADY_CLAIMED("ALBUM_ALREADY_CLAIMED", "该里程碑奖励已领取"),
    SERIES_NOT_FOUND("ALBUM_SERIES_NOT_FOUND", "系列不存在"),
    PROGRESS_NOT_FOUND("ALBUM_PROGRESS_NOT_FOUND", "图鉴进度不存在"),
    PROGRESS_SAVE_FAILED("ALBUM_PROGRESS_SAVE_FAILED", "图鉴进度保存失败"),
    MILESTONE_SAVE_FAILED("ALBUM_MILESTONE_SAVE_FAILED", "里程碑配置保存失败"),
    REWARD_SAVE_FAILED("ALBUM_REWARD_SAVE_FAILED", "奖励发放记录保存失败");

    private final String code;
    private final String message;

    AlbumErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getCode() { return code; }

    @Override
    public String getMessage() { return message; }
}