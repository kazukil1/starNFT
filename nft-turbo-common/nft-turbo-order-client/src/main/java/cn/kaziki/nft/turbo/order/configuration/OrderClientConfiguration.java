package cn.kaziki.nft.turbo.order.configuration;

import cn.kaziki.nft.turbo.api.goods.service.GoodsFacadeService;
import cn.kaziki.nft.turbo.api.inventory.service.InventoryFacadeService;
import cn.kaziki.nft.turbo.api.user.service.UserFacadeService;
import cn.kaziki.nft.turbo.order.sharding.id.WorkerIdHolder;
import cn.kaziki.nft.turbo.order.validator.*;
import cn.yueyu.nft.turbo.order.validator.*;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;


@Configuration
public class OrderClientConfiguration {

    @Bean
    public WorkerIdHolder workerIdHolder(RedissonClient redisson) {
        return new WorkerIdHolder(redisson);
    }

    /**
     * 这里为什么要使用prototype，详见文档：https://thoughts.aliyun.com/workspaces/6655879cf459b7001ba42f1b/docs/68a2e96151b1440001752e4f
     *
     * @param goodsFacadeService
     * @return
     */
    @Bean
    @Scope(value = BeanDefinition.SCOPE_PROTOTYPE)
    public GoodsValidator goodsValidator(GoodsFacadeService goodsFacadeService) {
        return new GoodsValidator(goodsFacadeService);
    }

    /**
     * 这里为什么要使用prototype，详见文档：https://thoughts.aliyun.com/workspaces/6655879cf459b7001ba42f1b/docs/68a2e96151b1440001752e4f
     *
     * @param inventoryFacadeService
     * @return
     */
    @Bean
    @Scope(value = BeanDefinition.SCOPE_PROTOTYPE)
    public StockValidator stockValidator(InventoryFacadeService inventoryFacadeService) {
        return new StockValidator(inventoryFacadeService);
    }

    /**
     * 这里为什么要使用prototype，详见文档：https://thoughts.aliyun.com/workspaces/6655879cf459b7001ba42f1b/docs/68a2e96151b1440001752e4f
     *
     * @param userFacadeService
     * @return
     */
    @Bean
    @Scope(value = BeanDefinition.SCOPE_PROTOTYPE)
    public UserValidator userValidator(UserFacadeService userFacadeService) {
        return new UserValidator(userFacadeService);
    }

    /**
     * 这里为什么要使用prototype，详见文档：https://thoughts.aliyun.com/workspaces/6655879cf459b7001ba42f1b/docs/68a2e96151b1440001752e4f
     *
     * @param goodsFacadeService
     * @return
     */
    @Bean
    @Scope(value = BeanDefinition.SCOPE_PROTOTYPE)
    public GoodsBookValidator goodsBookValidator(GoodsFacadeService goodsFacadeService) {
        return new GoodsBookValidator(goodsFacadeService);
    }

    @Bean
    @Primary
    public OrderCreateValidator orderValidatorChain(UserValidator userValidator, GoodsValidator goodsValidator, GoodsBookValidator goodsBookValidator) {
        userValidator.setNext(goodsValidator);
        goodsValidator.setNext(goodsBookValidator);
        return userValidator;
    }
}