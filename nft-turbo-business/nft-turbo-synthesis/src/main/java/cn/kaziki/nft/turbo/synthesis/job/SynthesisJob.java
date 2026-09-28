package cn.kaziki.nft.turbo.synthesis.job;

import cn.kaziki.nft.turbo.synthesis.domain.service.SynthesisService;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// 合成补偿定时任务
@Slf4j
@Component
public class SynthesisJob {

    @Autowired
    private SynthesisService synthesisService;

    // 扫描超时锁定的合成流水（每10分钟），解锁材料卡
    @XxlJob("scanTimeoutSynthesisLocks")
    public ReturnT<String> scanTimeoutLocks() {
        try {
            log.info("合成锁定超时扫描开始");
            synthesisService.scanTimeoutLocks();
            log.info("合成锁定超时扫描完成");
            return ReturnT.SUCCESS;
        } catch (Exception e) {
            log.error("合成锁定超时扫描失败", e);
            return new ReturnT<>(ReturnT.FAIL_CODE, e.getMessage());
        }
    }
}
