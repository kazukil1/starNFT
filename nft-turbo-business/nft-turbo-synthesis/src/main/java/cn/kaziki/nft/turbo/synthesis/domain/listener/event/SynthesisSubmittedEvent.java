package cn.kaziki.nft.turbo.synthesis.domain.listener.event;

import org.springframework.context.ApplicationEvent;

/**
 * 合成提交事件——Facade 提交完成后发布，触发异步执行合成链
 */
public class SynthesisSubmittedEvent extends ApplicationEvent {

    public SynthesisSubmittedEvent(String identifier) {
        super(identifier);
    }

    /**
     * 获取合成幂等号，Listener 凭此号重新加载 SynthesisStream
     */
    public String getIdentifier() {
        return (String) getSource();
    }
}
