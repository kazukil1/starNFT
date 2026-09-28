package cn.kaziki.nft.turbo.album.infrastructure.config;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 图鉴模块配置（对齐 BlindBoxListenerConfig 线程池模式）
 */
@Configuration
@EnableAsync
public class AlbumConfig {

    /** 图鉴进度刷新线程池：核心3/最大5/队列50（低频操作） */
    @Bean("albumExecutor")
    public ExecutorService albumExecutor() {
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
                .setNameFormat("albumListener-%d").build();

        return new ThreadPoolExecutor(3, 5,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(50),
                namedThreadFactory,
                new ThreadPoolExecutor.AbortPolicy());
    }
}
