package cn.kaziki.nft.turbo.sms;

import cn.kaziki.nft.turbo.sms.response.SmsSendResponse;

/**
 * 短信服务
 */
public interface SmsService {
    /**
     * 发送短信
     *
     * @param phoneNumber
     * @param code
     * @return
     */
    public SmsSendResponse sendMsg(String phoneNumber, String code);
}
