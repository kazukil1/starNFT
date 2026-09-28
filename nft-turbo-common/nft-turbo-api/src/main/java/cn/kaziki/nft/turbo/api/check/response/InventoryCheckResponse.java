package cn.kaziki.nft.turbo.api.check.response;

import cn.kaziki.nft.turbo.base.response.BaseResponse;
import lombok.Getter;
import lombok.Setter;

/**
 * 库存检查 响应
 */
@Getter
@Setter
public class InventoryCheckResponse extends BaseResponse {

    // 核对结果
    private Boolean checkResult;
}