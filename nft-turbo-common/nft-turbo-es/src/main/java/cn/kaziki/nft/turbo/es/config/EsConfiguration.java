package cn.kaziki.nft.turbo.es.config;

import org.dromara.easyes.starter.register.EsMapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * ES配置
 *
 */
@Configuration
@EsMapperScan("cn.yueyu.nft.turbo.*.infrastructure.mapper.es")
@ConditionalOnProperty(value = "easy-es.enable", havingValue = "true")
public class EsConfiguration {

}
