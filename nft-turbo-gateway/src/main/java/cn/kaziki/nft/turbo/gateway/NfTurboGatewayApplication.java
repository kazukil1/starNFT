package cn.kaziki.nft.turbo.gateway;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication(scanBasePackages = "cn.yueyu.nft.turbo.gateway")
public class NfTurboGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(NfTurboGatewayApplication.class, args);
    }

}
