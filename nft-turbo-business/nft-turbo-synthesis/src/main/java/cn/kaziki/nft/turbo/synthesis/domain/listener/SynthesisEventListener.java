package cn.kaziki.nft.turbo.synthesis.domain.listener;

import cn.kaziki.nft.turbo.synthesis.domain.entity.SynthesisStream;
import cn.kaziki.nft.turbo.synthesis.domain.listener.event.SynthesisSubmittedEvent;
import cn.kaziki.nft.turbo.synthesis.domain.service.SynthesisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 合成事件监听器——异步执行合成链（LOCKED → BURNING → BURNED → MINTING → MINTED）
 * <p>
 * 对齐 {@code BlindBoxEventListener} 的开盒异步模式：Controller 提交后立即返回，后台线程执行链操作。
 */
@Slf4j
@Component
public class SynthesisEventListener {

    @Autowired
    private SynthesisService synthesisService;

    @EventListener(value = SynthesisSubmittedEvent.class)
    @Async("synthesisExecutor")
    public void onSynthesisSubmitted(SynthesisSubmittedEvent event) {
        String identifier = event.getIdentifier();
        log.info("收到合成提交事件，开始异步执行，identifier={}", identifier);

        try {
            // 重新加载最新 DB 状态（防止 Facade 缓存与 DB 不一致）
            SynthesisStream stream = synthesisService.queryByIdentifier(identifier);
            if (stream == null) {
                log.error("合成流水不存在，identifier={}", identifier);
                return;
            }
            synthesisService.executeAsync(stream);
        } catch (Exception e) {
            log.error("合成异步执行异常，identifier={}", identifier, e);
        }
    }
}
