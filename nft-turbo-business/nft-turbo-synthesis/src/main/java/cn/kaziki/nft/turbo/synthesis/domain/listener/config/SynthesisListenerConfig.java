package cn.kaziki.nft.turbo.synthesis.domain.listener.config;

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
 * 合成异步执行线程池配置
 */
@Configuration
@EnableAsync
public class SynthesisListenerConfig {

    /**
     * 合成链执行线程池：核心5/最大10/队列100。
     * 合成属于低频操作（单个用户不会同时提交多个合成），核心线程数较小即可满足。
     */
    @Bean("synthesisExecutor")
    public ExecutorService synthesisExecutor() {

        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
                .setNameFormat("synthesisListener-%d").build();

        return new ThreadPoolExecutor(5, 10,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(100),
                namedThreadFactory,
                new ThreadPoolExecutor.AbortPolicy());
    }
}
