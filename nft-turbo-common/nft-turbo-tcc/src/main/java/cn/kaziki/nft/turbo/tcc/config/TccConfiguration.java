package cn.kaziki.nft.turbo.tcc.config;

import cn.kaziki.nft.turbo.tcc.service.TransactionLogService;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("cn.yueyu.nft.turbo.tcc.mapper")
public class TccConfiguration {

    @Bean
    public TransactionLogService transactionLogService() {
        return new TransactionLogService();
    }
}
