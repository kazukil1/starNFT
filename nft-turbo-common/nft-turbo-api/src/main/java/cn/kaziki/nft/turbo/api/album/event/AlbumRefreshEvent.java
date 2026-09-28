package cn.kaziki.nft.turbo.api.album.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 图鉴进度刷新事件 — 购卡/开盒/转赠/合成/空投 后发布，异步刷新用户系列收集进度
 */
@Getter
public class AlbumRefreshEvent extends ApplicationEvent {

    private final String userId;
    private final Long seriesId;

    public AlbumRefreshEvent(Object source, String userId, Long seriesId) {
        super(source);
        this.userId = userId;
        this.seriesId = seriesId;
    }
}
