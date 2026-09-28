package cn.kaziki.nft.turbo.box.domain.listener.event;

import org.springframework.context.ApplicationEvent;

/**
 * 开启盲盒事件
 */
public class BlindBoxOpenEvent extends ApplicationEvent {

    public BlindBoxOpenEvent(Long blindBoxItemId) {
        super(blindBoxItemId);
    }
}
