package cn.kaziki.nft.turbo.box;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"cn.yueyu.nft.turbo.box"})
@EnableDubbo
public class NftTurboBoxApplication {

	public static void main(String[] args) {
		SpringApplication.run(NftTurboBoxApplication.class, args);
	}

}
