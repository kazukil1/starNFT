package cn.kaziki.nft.turbo.api.star.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * 星尘流水
 */
@Getter
@Setter
@ToString
public class StarStreamVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 变更类型：PURCHASE/BOX_DROP/GIFT/TASK/AIRDROP/SPEND */
    private String changeType;

    /** 变更数量（正为获得，负为消耗） */
    private Long changeAmount;

    /** 变更后余额 */
    private Long balanceAfter;

    /** 业务单据号 */
    private String bizNo;

    /** 业务类型 */
    private String bizType;

    /** 变更时间 */
    private Date gmtCreate;
}
