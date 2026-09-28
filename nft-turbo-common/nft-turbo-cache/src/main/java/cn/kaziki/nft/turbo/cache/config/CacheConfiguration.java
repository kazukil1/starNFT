package cn.kaziki.nft.turbo.cache.config;

import com.alicp.jetcache.anno.config.EnableMethodCache;
import org.springframework.context.annotation.Configuration;

/**
 * 缓存配置
 */
@Configuration
@EnableMethodCache(basePackages = "cn.yueyu.nft.turbo")
public class CacheConfiguration {
}
