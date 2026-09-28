package cn.kaziki.nft.turbo.api.notice.service;


import cn.kaziki.nft.turbo.api.notice.response.NoticeResponse;

/**
 * 认证rpc服务
 */
public interface NoticeFacadeService {
    /**
     * 生成并发送短信验证码
     */
    public NoticeResponse generateAndSendSmsCaptcha(String telephone);
}
