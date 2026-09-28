package cn.kaziki.nft.turbo.goods;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"cn.yueyu.nft.turbo.goods","cn.yueyu.nft.turbo.collection","cn.yueyu.nft.turbo.box","cn.yueyu.nft.turbo.star","cn.yueyu.nft.turbo.synthesis","cn.yueyu.nft.turbo.series","cn.yueyu.nft.turbo.album"})
@EnableDubbo(scanBasePackages = {"cn.yueyu.nft.turbo.goods","cn.yueyu.nft.turbo.collection","cn.yueyu.nft.turbo.box","cn.yueyu.nft.turbo.star","cn.yueyu.nft.turbo.synthesis","cn.yueyu.nft.turbo.series","cn.yueyu.nft.turbo.album"})
public class NfTurboGoodsApplication {

    public static void main(String[] args) {
        SpringApplication.run(NfTurboGoodsApplication.class, args);
    }

}
