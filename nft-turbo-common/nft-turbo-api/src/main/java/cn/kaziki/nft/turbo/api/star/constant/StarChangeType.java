package cn.kaziki.nft.turbo.api.star.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 星尘变更类型
 */
@AllArgsConstructor
@Getter
public enum StarChangeType {

    PURCHASE("PURCHASE", "闪购购买"),
    BOX_DROP("BOX_DROP", "盲盒掉落"),
    GIFT("GIFT", "购卡赠送"),
    TASK("TASK", "任务/签到"),
    AIRDROP("AIRDROP", "运营空投"),
    ALBUM_REWARD("ALBUM_REWARD", "图鉴奖励"),
    SPEND("SPEND", "合成消耗");

    private final String code;
    private final String value;
}
