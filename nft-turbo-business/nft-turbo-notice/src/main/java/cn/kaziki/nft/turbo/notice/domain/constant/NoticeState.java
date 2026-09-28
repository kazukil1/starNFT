package cn.kaziki.nft.turbo.notice.domain.constant;

/**
 * 通知 状态
 */
public enum NoticeState {
    // 初始化
    INIT,

    // 已发送成功
    SUCCESS,

    // 发送失败
    FAILED,

    // 已挂起
    SUSPENDED;
}
