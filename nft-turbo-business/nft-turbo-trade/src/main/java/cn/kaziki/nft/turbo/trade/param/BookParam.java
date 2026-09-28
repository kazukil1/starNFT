package cn.kaziki.nft.turbo.trade.param;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 预约 入参
 */
@Getter
@Setter
public class BookParam {

    @NotNull(message = "goodsId is null")
    private String goodsId;

    @NotNull(message = "goodsType is null")
    private String goodsType;

}
