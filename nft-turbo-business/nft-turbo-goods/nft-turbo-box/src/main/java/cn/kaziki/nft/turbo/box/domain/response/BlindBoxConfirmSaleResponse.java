package cn.kaziki.nft.turbo.box.domain.response;

import cn.kaziki.nft.turbo.base.response.BaseResponse;
import cn.kaziki.nft.turbo.box.domain.entity.BlindBox;
import cn.kaziki.nft.turbo.box.domain.entity.BlindBoxItem;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class BlindBoxConfirmSaleResponse extends BaseResponse {

    private BlindBox blindBox;

    private BlindBoxItem blindBoxItem;
}
