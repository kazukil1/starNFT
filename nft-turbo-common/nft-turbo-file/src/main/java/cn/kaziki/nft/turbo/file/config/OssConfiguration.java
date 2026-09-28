package cn.kaziki.nft.turbo.file.config;

import cn.kaziki.nft.turbo.file.FileService;
import cn.kaziki.nft.turbo.file.OssServiceImpl;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties(OssProperties.class)
public class OssConfiguration {

    @Autowired
    private OssProperties properties;

    @PostConstruct
    public void init() {
        log.info("OSS 配置加载情况 — enabled={}, bucket={}, endPoint={}, accessKey={}, accessSecret={}",
                properties.isEnabled(),
                properties.getBucket(),
                properties.getEndPoint(),
                mask(properties.getAccessKey()),
                mask(properties.getAccessSecret()));
    }

    @Bean
    @ConditionalOnMissingBean
    public FileService ossService() {
        if (!properties.isEnabled() || StringUtils.isAnyBlank(
                properties.getBucket(), properties.getEndPoint(),
                properties.getAccessKey(), properties.getAccessSecret())) {
            String msg = String.format(
                    "OSS 凭证不完整，无法创建 OssServiceImpl。"
                    + "请检查 Nacos 配置中是否包含 spring.oss.* 属性。"
                    + "当前值: enabled=%s, bucket=%s, endPoint=%s, accessKey=%s, accessSecret=%s",
                    properties.isEnabled(),
                    properties.getBucket(),
                    properties.getEndPoint(),
                    mask(properties.getAccessKey()),
                    mask(properties.getAccessSecret()));
            log.error(msg);
            throw new IllegalStateException(msg);
        }
        OssServiceImpl ossService = new OssServiceImpl();
        ossService.setBucket(properties.getBucket());
        ossService.setEndPoint(properties.getEndPoint());
        ossService.setAccessKey(properties.getAccessKey());
        ossService.setAccessSecret(properties.getAccessSecret());
        log.info("OSS 服务已初始化: bucket={}, endPoint={}", properties.getBucket(), properties.getEndPoint());
        return ossService;
    }

    private static String mask(String value) {
        if (value == null) return "null";
        if (value.length() <= 4) return "****";
        return value.substring(0, 4) + "****";
    }
}
