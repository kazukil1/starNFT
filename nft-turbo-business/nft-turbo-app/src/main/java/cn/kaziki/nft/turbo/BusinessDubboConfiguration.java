package cn.kaziki.nft.turbo;

import cn.kaziki.nft.turbo.api.album.service.AlbumFacadeService;
import cn.kaziki.nft.turbo.api.box.service.BlindBoxManageFacadeService;
import cn.kaziki.nft.turbo.api.box.service.BlindBoxReadFacadeService;
import cn.kaziki.nft.turbo.api.chain.service.ChainFacadeService;
import cn.kaziki.nft.turbo.api.collection.service.CollectionReadFacadeService;
import cn.kaziki.nft.turbo.api.collection.service.SeriesManageFacadeService;
import cn.kaziki.nft.turbo.api.collection.service.SeriesReadFacadeService;
import cn.kaziki.nft.turbo.api.goods.service.GoodsFacadeService;
import cn.kaziki.nft.turbo.api.inventory.service.InventoryFacadeService;
import cn.kaziki.nft.turbo.api.order.service.OrderFacadeService;
import cn.kaziki.nft.turbo.api.pay.service.PayFacadeService;
import cn.kaziki.nft.turbo.api.star.service.StarAccountFacadeService;
import cn.kaziki.nft.turbo.api.star.service.StarManageFacadeService;
import cn.kaziki.nft.turbo.api.star.service.StarReadFacadeService;
import cn.kaziki.nft.turbo.api.synthesis.service.SynthesisFacadeService;
import cn.kaziki.nft.turbo.api.user.service.UserFacadeService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * dubbo配置
 *
 * @author Hollis
 */
@Configuration
public class BusinessDubboConfiguration {

    @DubboReference(version = "1.0.0")
    private ChainFacadeService chainFacadeService;

    @DubboReference(version = "1.0.0")
    private OrderFacadeService orderFacadeService;

    @DubboReference(version = "1.0.0")
    private PayFacadeService payFacadeService;

    @DubboReference(version = "1.0.0")
    private UserFacadeService userFacadeService;

    @DubboReference(version = "1.0.0")
    private CollectionReadFacadeService collectionReadFacadeService;

    @DubboReference(version = "1.0.0")
    private GoodsFacadeService goodsFacadeService;

    @DubboReference(version = "1.0.0")
    private InventoryFacadeService inventoryFacadeService;

    @DubboReference(version = "1.0.0")
    private BlindBoxManageFacadeService blindBoxManageFacadeService;

    @DubboReference(version = "1.0.0")
    private BlindBoxReadFacadeService blindBoxReadFacadeService;

    @DubboReference(version = "1.0.0")
    private SynthesisFacadeService synthesisFacadeService;

    @DubboReference(version = "1.0.0")
    private SeriesReadFacadeService seriesReadFacadeService;

    @DubboReference(version = "1.0.0")
    private SeriesManageFacadeService seriesManageFacadeService;

    @DubboReference(version = "1.0.0")
    private AlbumFacadeService albumFacadeService;

    @DubboReference(version = "1.0.0")
    private StarAccountFacadeService starAccountFacadeService;

    @DubboReference(version = "1.0.0")
    private StarManageFacadeService starManageFacadeService;

    @DubboReference(version = "1.0.0")
    private StarReadFacadeService starReadFacadeService;

    @Bean
    @ConditionalOnMissingBean(name = "collectionFacadeService")
    public CollectionReadFacadeService collectionFacadeService() {
        return collectionReadFacadeService;
    }

    @Bean
    @ConditionalOnMissingBean(name = "userFacadeService")
    public UserFacadeService userFacadeService() {
        return userFacadeService;
    }


    @Bean
    @ConditionalOnMissingBean(name = "payFacadeService")
    public PayFacadeService payFacadeService() {
        return payFacadeService;
    }


    @Bean
    @ConditionalOnMissingBean(name = "orderFacadeService")
    public OrderFacadeService orderFacadeService() {
        return orderFacadeService;
    }

    @Bean
    @ConditionalOnMissingBean(name = "chainFacadeService")
    public ChainFacadeService chainFacadeService() {
        return chainFacadeService;
    }

    @Bean
    @ConditionalOnMissingBean(name = "goodsFacadeService")
    public GoodsFacadeService goodsFacadeService() {
        return goodsFacadeService;
    }

    @Bean
    @ConditionalOnMissingBean(name = "inventoryFacadeService")
    public InventoryFacadeService inventoryFacadeService() {
        return inventoryFacadeService;
    }

    @Bean
    @ConditionalOnMissingBean(name = "blindBoxManageFacadeService")
    public BlindBoxManageFacadeService blindBoxManageFacadeService() {
        return blindBoxManageFacadeService;
    }

    @Bean
    @ConditionalOnMissingBean(name = "blindBoxReadFacadeService")
    public BlindBoxReadFacadeService blindBoxReadFacadeService() {
        return blindBoxReadFacadeService;
    }

    @Bean
    @ConditionalOnMissingBean(name = "synthesisFacadeService")
    public SynthesisFacadeService synthesisFacadeService() {
        return synthesisFacadeService;
    }

    @Bean
    @ConditionalOnMissingBean(name = "seriesReadFacadeService")
    public SeriesReadFacadeService seriesReadFacadeService() {
        return seriesReadFacadeService;
    }

    @Bean
    @ConditionalOnMissingBean(name = "seriesManageFacadeService")
    public SeriesManageFacadeService seriesManageFacadeService() {
        return seriesManageFacadeService;
    }

    @Bean
    @ConditionalOnMissingBean(name = "albumFacadeService")
    public AlbumFacadeService albumFacadeService() {
        return albumFacadeService;
    }

    @Bean
    @ConditionalOnMissingBean(name = "starAccountFacadeService")
    public StarAccountFacadeService starAccountFacadeService() {
        return starAccountFacadeService;
    }

    @Bean
    @ConditionalOnMissingBean(name = "starManageFacadeService")
    public StarManageFacadeService starManageFacadeService() {
        return starManageFacadeService;
    }

    @Bean
    @ConditionalOnMissingBean(name = "starReadFacadeService")
    public StarReadFacadeService starReadFacadeService() {
        return starReadFacadeService;
    }

}
