package cn.kaziki.nft.turbo.album.listener;

import cn.kaziki.nft.turbo.album.domain.service.AlbumService;
import cn.kaziki.nft.turbo.api.album.event.AlbumRefreshEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 图鉴进度刷新监听器 — 异步处理，不阻塞主流程
 */
@Slf4j
@Component
public class AlbumProgressListener {

    @Autowired
    private AlbumService albumService;

    @EventListener(value = AlbumRefreshEvent.class)
    @Async("albumExecutor")
    public void onAlbumRefresh(AlbumRefreshEvent event) {
        try {
            albumService.refreshProgress(event.getUserId(), event.getSeriesId());
        } catch (Exception e) {
            log.error("图鉴进度刷新失败，userId={}，seriesId={}", event.getUserId(), event.getSeriesId(), e);
        }
    }
}
