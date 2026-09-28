package cn.kaziki.nft.turbo.collection.exception;

import cn.kaziki.nft.turbo.base.exception.ErrorCode;

/**
 * 藏品相关错误码
 */
public enum CollectionErrorCode implements ErrorCode {
    /**
     * 藏品相关错误
     */
    COLLECTION_SAVE_FAILED("COLLECTION_SAVE_FAILED", "藏品信息保存失败"),

    COLLECTION_UPDATE_FAILED("COLLECTION_UPDATE_FAILED", "藏品信息更新失败"),
    /**
     * 用户相关错误
     */
    COLLECTION_USER_QUERY_FAIL("COLLECTION_USER_QUERY_FAIL", "查询用户信息失败"),
    /**
     * 藏品持有相关错误
     */
    HELD_COLLECTION_SAVE_FAILED("HELD_COLLECTION_SAVE_FAILED", "藏品持有信息保存失败"),

    /**
     * 藏品持有用户校验错误
     */
    HELD_COLLECTION_OWNER_CHECK_ERROR("HELD_COLLECTION_OWNER_CHECK_ERROR", "藏品持有用户校验错误"),

    /**
     * 藏品状态校验错误
     */
    HELD_COLLECTION_STATE_CHECK_ERROR("HELD_COLLECTION_STATE_CHECK_ERROR", "藏品状态校验错误"),

    /**
     * 藏品相关错误
     */
    COLLECTION_QUERY_FAIL("COLLECTION_QUERY_FAIL", "查询藏品信息失败"),

    /**
     * 藏品不存在
     */
    COLLECTION_NOT_EXIST("COLLECTION_NOT_EXIST", "藏品不存在"),
    /**
     * 藏品持有相关错误
     */
    HELD_COLLECTION_QUERY_FAIL("HELD_COLLECTION_QUERY_FAIL", "查询持有藏品信息失败"),
    /**
     * 藏品库存相关错误
     */
    COLLECTION_INVENTORY_UPDATE_FAILED("COLLECTION_INVENTORY_UPDATE_FAILED", "藏品库存更新失败"),
    /**
     * 藏品流水信息保存失败
     */
    COLLECTION_STREAM_SAVE_FAILED("COLLECTION_STREAM_SAVE_FAILED", "藏品流水信息保存失败"),
    /**
     * 藏品库存流水信息保存失败
     */
    COLLECTION_INVENTORY_STREAM_SAVE_FAILED("COLLECTION_INVENTORY_STREAM_SAVE_FAILED", "藏品库存流水信息保存失败"),
    /**
     * 藏品快照信息保存失败
     */
    COLLECTION_SNAPSHOT_SAVE_FAILED("COLLECTION_SNAPSHOT_SAVE_FAILED", "藏品快照信息保存失败"),

    /**
     * 藏品持有流水信息保存失败
     */
    HELD_COLLECTION_STREAM_SAVE_FAILED("HELD_COLLECTION_STREAM_SAVE_FAILED", "藏品持有流水信息保存失败"),
    /**
     * 藏品流水信息已存在
     */
    COLLECTION_STREAM_EXIST("COLLECTION_STREAM_EXIST", "藏品流水信息已存在"),

    /**
     * 未冻结库存无法解冻
     */
    INVENTORY_UNFREEZE_FAILED("INVENTORY_UNFREEZE_FAILED", "未冻结库存无法解冻"),
    /**
     * 藏品空投流水更新失败
     */
    COLLECTION_AIRDROP_STREAM_UPDATE_FAILED("COLLECTION_AIRDROP_STREAM_UPDATE_FAILED", "藏品空投流水更新失败"),

    /**
     * 系列不存在
     */
    SERIES_NOT_EXIST("SERIES_NOT_EXIST", "藏品系列不存在"),
    /**
     * 系列信息保存失败
     */
    SERIES_SAVE_FAILED("SERIES_SAVE_FAILED", "藏品系列信息保存失败"),
    /**
     * 稀有度评定不可下调
     */
    COLLECTION_RARITY_DOWNGRADE_FORBIDDEN("COLLECTION_RARITY_DOWNGRADE_FORBIDDEN", "稀有度评定公示后不可下调"),
    /**
     * 已归入系列不可更换
     */
    COLLECTION_SERIES_CHANGE_FORBIDDEN("COLLECTION_SERIES_CHANGE_FORBIDDEN", "藏品已归入系列，不可更换系列");


    private String code;

    private String message;

    CollectionErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getCode() {
        return this.code;
    }

    @Override
    public String getMessage() {
        return this.message;
    }
}
