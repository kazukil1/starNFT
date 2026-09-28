package cn.kaziki.nft.turbo.chain.domain.entity;

import cn.kaziki.nft.turbo.chain.domain.constant.ChainOperateStateEnum;
import cn.kaziki.nft.turbo.datasource.domain.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * 区块链操作信息实体
 */
@Getter
@Setter
public class ChainOperateInfo extends BaseEntity {

    // 链类型 @see ChainType
    private String chainType;

    // 业务id
    private String bizId;

    // 业务类型 @see ChainOperateBizTypeEnum
    private String bizType;

    // 操作类型 @see ChainOperateTypeEnum
    private String operateType;

    // 状态
    private ChainOperateStateEnum state;

    // 操作时间
    private Date operateTime;

    // 成功时间
    private Date succeedTime;

    // 入参
    private String param;

    // 返回结果
    private String result;

    // 外部业务id
    private String outBizId;
}
