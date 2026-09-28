package cn.kaziki.nft.turbo.admin;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"cn.yueyu.nft.turbo.admin"})
@EnableDubbo
public class NfTurboAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(NfTurboAdminApplication.class, args);
    }

}
