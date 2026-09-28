package cn.kaziki.nft.turbo.api.collection.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 藏品获取途径类型
 * <p>
 * 双重用途：
 * <ul>
 *   <li><b>collection.obtain_type</b>：藏品可供哪些渠道获取（逗号分隔多选）</li>
 *   <li><b>held_collection.obtain_type</b>：用户实际通过哪种途径获得（单值）</li>
 * </ul>
 */
@AllArgsConstructor
@Getter
public enum CollectionObtainType {

    /** 直售购买 */
    DIRECT_SALE("DIRECT_SALE", "直接购买"),

    /** 盲盒开出 */
    BLIND_BOX("BLIND_BOX", "盲盒获取"),

    /** 合成产出 */
    SYNTHESIS("SYNTHESIS", "合成产出"),

    /** 转赠接收 */
    TRANSFER("TRANSFER", "转赠接收"),

    /** 空投 */
    AIRDROP("AIRDROP", "空投");

    private final String code;
    private final String label;

    /** collection.obtain_type 默认值（仅直接购买） */
    public static final String DEFAULT_COLLECTION_CHANNELS = "DIRECT_SALE";

    /**
     * 逗号分隔字符串 → 是否包含指定渠道
     * @param obtainType collection.obtain_type 列值（逗号分隔）
     * @param channel   待检测的渠道
     */
    public static boolean contains(String obtainType, CollectionObtainType channel) {
        if (obtainType == null || obtainType.isBlank()) {
            return false;
        }
        for (String part : obtainType.split(",")) {
            if (part.trim().equalsIgnoreCase(channel.getCode())) {
                return true;
            }
        }
        return false;
    }
}
