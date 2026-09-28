package cn.kaziki.nft.turbo.star.domain.entity;

import cn.kaziki.nft.turbo.api.star.request.StarChangeRequest;
import cn.kaziki.nft.turbo.datasource.domain.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 星尘账户流水 — 记录用户星尘余额的每一次变动
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("star_account_stream")
public class StarAccountStream extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 幂等号 */
    private String identifier;

    /** 用户ID */
    private String userId;

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

    /** 扩展信息 */
    private String extendInfo;

    /** 账户初始化工单（参照 CollectionStream 快照模式） */
    public static StarAccountStream ofInit(StarAccount account) {
        StarAccountStream stream = new StarAccountStream();
        stream.setIdentifier("ACCOUNT_INIT_" + account.getUserId());
        stream.setUserId(account.getUserId());
        stream.setChangeType("INIT");
        stream.setChangeAmount(0L);
        stream.setBalanceAfter(0L);
        return stream;
    }

    /** 账户变更流水（参照 CollectionStream 快照模式） */
    public static StarAccountStream of(StarAccount account, StarChangeRequest request, Long balanceAfter) {
        StarAccountStream stream = new StarAccountStream();
        stream.setIdentifier(request.getIdentifier());
        stream.setUserId(account.getUserId());
        stream.setChangeType(request.getChangeType().getCode());
        stream.setChangeAmount(request.getAmount());
        stream.setBalanceAfter(balanceAfter);
        stream.setBizNo(request.getBizNo());
        stream.setBizType(request.getBizType());
        return stream;
    }
}
