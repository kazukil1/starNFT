package cn.kaziki.nft.turbo.api.synthesis.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * 合成广播消息体（SP/UR 合成完成时通过 MQ 推送至全平台公告）
 */
@Getter
@Setter
@ToString
public class SynthesisBroadcastVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 合成者昵称（脱敏），如 "冰*心" */
    private String userNickName;

    /** 系列名称 */
    private String seriesName;

    /** 产物稀有度 */
    private String targetRarity;

    /** 产物藏品名称 */
    private String productName;

    /** 产物编号 */
    private String serialNo;
}
