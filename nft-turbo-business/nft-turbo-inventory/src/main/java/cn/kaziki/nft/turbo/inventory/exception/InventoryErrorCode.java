package cn.kaziki.nft.turbo.inventory.exception;

import cn.kaziki.nft.turbo.base.exception.ErrorCode;


public enum InventoryErrorCode implements ErrorCode {

    /**
     * 库存更新失败
     */
    INVENTORY_UPDATE_FAILED("INVENTORY_UPDATE_FAILED", "库存更新失败"),

    /**
     * 库存查询失败
     */
    INVENTORY_QUERY_FAILED("INVENTORY_QUERY_FAILED", "库存查询失败");

    private String code;

    private String message;

    InventoryErrorCode(String code, String message) {
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
