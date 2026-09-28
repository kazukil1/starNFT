package cn.kaziki.nft.turbo.sms;

import cn.kaziki.nft.turbo.lock.DistributeLock;
import cn.kaziki.nft.turbo.sms.response.SmsSendResponse;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mock短信服务
 *
 */
@Slf4j
@Setter
public class MockSmsServiceImpl implements SmsService {

    private static Logger logger = LoggerFactory.getLogger(MockSmsServiceImpl.class);

    @DistributeLock(scene = "SEND_SMS", keyExpression = "#phoneNumber")
    @Override
    public SmsSendResponse sendMsg(String phoneNumber, String code) {
        SmsSendResponse smsSendResponse = new SmsSendResponse();
        smsSendResponse.setSuccess(true);
        return smsSendResponse;
    }
}
