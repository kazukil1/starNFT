package cn.kaziki.nft.turbo.star.job;

import cn.kaziki.nft.turbo.api.star.service.StarManageFacadeService;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// 星尘每日库存初始化定时任务
@Slf4j
@Component
public class StarInventoryJob {

    @Autowired
    private StarManageFacadeService starManageFacadeService;

    @XxlJob("initStarDailyInventory")
    public ReturnT<String> initStarDailyInventory() {
        try {
            log.info("星尘库存初始化任务开始");
            starManageFacadeService.initTodayInventory();
            log.info("星尘库存初始化任务完成");
            return ReturnT.SUCCESS;
        } catch (Exception e) {
            log.error("星尘库存初始化任务失败", e);
            return new ReturnT<>(ReturnT.FAIL_CODE, e.getMessage());
        }
    }
}
