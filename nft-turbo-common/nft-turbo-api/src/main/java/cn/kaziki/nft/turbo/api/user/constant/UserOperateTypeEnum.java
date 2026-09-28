package cn.kaziki.nft.turbo.api.user.constant;

import org.apache.http.auth.AUTH;

/**
 * 用户操作类型
 */
public enum UserOperateTypeEnum {
    // 冻结
    FREEZE,

    // 解冻
    UNFREEZE,

    // 注册
    REGISTER,

    // 激活
    ACTIVE,

    // 实名认证
    AUTH,

    // 修改信息
    MODIFY;
}
