package cn.kaziki.nft.turbo.api.pay.request;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Hollis
 */
@Getter
@Setter
public class PayQueryByBizNo implements PayQueryCondition {

    private String bizNo;

    private String bizType;
}
