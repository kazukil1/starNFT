package cn.kaziki.nft.turbo.inventory.facade;

import cn.kaziki.nft.turbo.api.goods.constant.GoodsType;
import cn.kaziki.nft.turbo.api.inventory.request.InventoryRequest;
import cn.kaziki.nft.turbo.api.inventory.service.InventoryFacadeService;
import cn.kaziki.nft.turbo.base.response.MultiResponse;
import cn.kaziki.nft.turbo.base.response.SingleResponse;
import cn.kaziki.nft.turbo.inventory.domain.response.InventoryResponse;
import cn.kaziki.nft.turbo.inventory.domain.service.impl.BlindBoxInventoryRedisService;
import cn.kaziki.nft.turbo.inventory.domain.service.impl.CollectionInventoryRedisService;
import cn.kaziki.nft.turbo.inventory.domain.service.impl.StarInventoryRedisService;
import com.alibaba.csp.sentinel.SphO;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static cn.kaziki.nft.turbo.api.common.constant.CommonConstant.SEPARATOR;
import static cn.kaziki.nft.turbo.inventory.domain.service.impl.AbstractInventoryRedisService.ERROR_CODE_INVENTORY_IS_ZERO;
import static cn.kaziki.nft.turbo.inventory.domain.service.impl.AbstractInventoryRedisService.ERROR_CODE_INVENTORY_NOT_ENOUGH;
import static cn.kaziki.nft.turbo.inventory.exception.InventoryErrorCode.INVENTORY_QUERY_FAILED;

@DubboService(version = "1.0.0")
@Slf4j
public class InventoryFacadeServiceImpl implements InventoryFacadeService {

    @Autowired
    private CollectionInventoryRedisService collectionInventoryRedisService;

    @Autowired
    private BlindBoxInventoryRedisService blindBoxInventoryRedisService;

    @Autowired
    private StarInventoryRedisService starInventoryRedisService;
    //商品类型不支持
    private static final String ERROR_CODE_UNSUPPORTED_GOODS_TYPE = "UNSUPPORTED_GOODS_TYPE";

    private Cache<String, Boolean> soldOutGoodsLocalCache;

    @PostConstruct
    public void init() {
        soldOutGoodsLocalCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(3000)
                .build();
    }

    // 初始化redis库存
    @Override
    public SingleResponse<Boolean> init(InventoryRequest inventoryRequest) {
        GoodsType goodsType = inventoryRequest.getGoodsType();
        InventoryResponse inventoryResponse = switch (goodsType) {
            case COLLECTION -> collectionInventoryRedisService.init(inventoryRequest);

            case BLIND_BOX -> blindBoxInventoryRedisService.init(inventoryRequest);

            case STAR -> starInventoryRedisService.init(inventoryRequest);

            default -> throw new UnsupportedOperationException(ERROR_CODE_UNSUPPORTED_GOODS_TYPE);
        };

        if (inventoryResponse.getSuccess()) {
            return SingleResponse.of(true);
        }

        return SingleResponse.fail(inventoryResponse.getResponseCode(), inventoryResponse.getResponseMessage());
    }

    // 减少redis库存
    @Override
    public SingleResponse<Boolean> decrease(InventoryRequest inventoryRequest) {
        if (SphO.entry("INVENTORY_DECREASE")) {
            try {
                GoodsType goodsType = inventoryRequest.getGoodsType();

                // 查看 商品售空 缓存是否存在记录，在此处直接拦住请求
                if (soldOutGoodsLocalCache.getIfPresent(goodsType + SEPARATOR + inventoryRequest.getGoodsId()) != null) {
                    return SingleResponse.fail(ERROR_CODE_INVENTORY_NOT_ENOUGH, "库存不足");
                }

                InventoryResponse inventoryResponse = switch (goodsType) {
                    case COLLECTION -> collectionInventoryRedisService.decrease(inventoryRequest);

                    case BLIND_BOX -> blindBoxInventoryRedisService.decrease(inventoryRequest);

                    case STAR -> starInventoryRedisService.decrease(inventoryRequest);

                    default -> throw new UnsupportedOperationException(ERROR_CODE_UNSUPPORTED_GOODS_TYPE);
                };

                // 判断库存为0，则在本地缓存记录，用于对售罄商品快速决策
                if (isSoldOut(inventoryResponse)) {
                    soldOutGoodsLocalCache.put(goodsType + SEPARATOR + inventoryRequest.getGoodsId(), true);
                }

                if (!inventoryResponse.getSuccess()) {
                    return SingleResponse.fail(inventoryResponse.getResponseCode(), inventoryResponse.getResponseMessage());
                }

                return SingleResponse.of(true);
            } finally {
                SphO.exit();
            }
        } else {
            log.warn("INVENTORY_DECREASE 触发限流...");
            return SingleResponse.of(false);
        }
    }

    private static boolean isSoldOut(InventoryResponse inventoryResponse) {
        if (inventoryResponse.getSuccess() && inventoryResponse.getInventory() == 0) {
            //这部分代码没有实际功能作用，仅用于日志埋点，方便压测时判断延时，详见压测相关视频
            log.warn("debug:soldOut ...");
        }
        // 情况一：缓存库存扣减成功，且剩余缓存库存为0
        // 情况二：缓存库存扣减失败，缓存库存已经为0
        return inventoryResponse.getSuccess() && inventoryResponse.getInventory() == 0
                || !inventoryResponse.getSuccess() && inventoryResponse.getResponseCode().equals(ERROR_CODE_INVENTORY_IS_ZERO);
    }

    // 增加库存
    @Override
    public SingleResponse<Boolean> increase(InventoryRequest inventoryRequest) {
        GoodsType goodsType = inventoryRequest.getGoodsType();
        InventoryResponse inventoryResponse = switch (goodsType) {
            case COLLECTION -> collectionInventoryRedisService.increase(inventoryRequest);

            case BLIND_BOX -> blindBoxInventoryRedisService.increase(inventoryRequest);

            case STAR -> starInventoryRedisService.increase(inventoryRequest);

            default -> throw new UnsupportedOperationException(ERROR_CODE_UNSUPPORTED_GOODS_TYPE);
        };

        if (inventoryResponse.getSuccess()) {
            //如果库存大于0，则清除本地缓存中的商品售罄标记
            //但是因为是本地缓存，所以无法保证一致性，极端情况下，会存在一分钟的数据不一致的延迟。但是在高并发秒杀场景下，一般是不允许修改库存，所以这种不一致业务上可接受
            if (inventoryResponse.getInventory() != null && inventoryResponse.getInventory() > 0) {
                soldOutGoodsLocalCache.invalidate(goodsType + SEPARATOR + inventoryRequest.getGoodsId());
            }

            return SingleResponse.of(true);
        }

        return SingleResponse.fail(inventoryResponse.getResponseCode(), inventoryResponse.getResponseMessage());
    }

    // 删除redis
    @Override
    public SingleResponse<Void> invalid(InventoryRequest inventoryRequest) {
        GoodsType goodsType = inventoryRequest.getGoodsType();
        switch (goodsType) {
            case COLLECTION -> collectionInventoryRedisService.invalid(inventoryRequest);

            case BLIND_BOX -> blindBoxInventoryRedisService.invalid(inventoryRequest);

            case STAR -> starInventoryRedisService.invalid(inventoryRequest);

            default -> throw new UnsupportedOperationException(ERROR_CODE_UNSUPPORTED_GOODS_TYPE);
        }

        soldOutGoodsLocalCache.invalidate(goodsType + SEPARATOR + inventoryRequest.getGoodsId());

        return SingleResponse.of(null);
    }


    // 查询库存扣减流水
    @Override
    public SingleResponse<String> getInventoryDecreaseLog(InventoryRequest inventoryRequest) {
        GoodsType goodsType = inventoryRequest.getGoodsType();
        String inventoryResponse = switch (goodsType) {
            case COLLECTION -> collectionInventoryRedisService.getInventoryDecreaseLog(inventoryRequest);

            case BLIND_BOX -> blindBoxInventoryRedisService.getInventoryDecreaseLog(inventoryRequest);

            case STAR -> starInventoryRedisService.getInventoryDecreaseLog(inventoryRequest);

            default -> throw new UnsupportedOperationException(ERROR_CODE_UNSUPPORTED_GOODS_TYPE);
        };

        return SingleResponse.of(inventoryResponse);
    }

    // 查询库存增加流水
    @Override
    public SingleResponse<String> getInventoryIncreaseLog(InventoryRequest inventoryRequest) {
        GoodsType goodsType = inventoryRequest.getGoodsType();
        String inventoryResponse = switch (goodsType) {
            case COLLECTION -> collectionInventoryRedisService.getInventoryIncreaseLog(inventoryRequest);

            case BLIND_BOX -> blindBoxInventoryRedisService.getInventoryIncreaseLog(inventoryRequest);

            case STAR -> starInventoryRedisService.getInventoryIncreaseLog(inventoryRequest);

            default -> throw new UnsupportedOperationException(ERROR_CODE_UNSUPPORTED_GOODS_TYPE);
        };

        return SingleResponse.of(inventoryResponse);
    }

    // 批量查询库存扣减流水
    @Override
    public MultiResponse<String> getInventoryDecreaseLogs(InventoryRequest inventoryRequest) {
        GoodsType goodsType = inventoryRequest.getGoodsType();
        List<String> inventoryResponse = switch (goodsType) {
            case COLLECTION -> collectionInventoryRedisService.getInventoryDecreaseLogs(inventoryRequest);

            case BLIND_BOX -> blindBoxInventoryRedisService.getInventoryDecreaseLogs(inventoryRequest);

            case STAR -> starInventoryRedisService.getInventoryDecreaseLogs(inventoryRequest);

            default -> throw new UnsupportedOperationException(ERROR_CODE_UNSUPPORTED_GOODS_TYPE);
        };

        return MultiResponse.of(Objects.requireNonNullElse(inventoryResponse, Collections.emptyList()));
    }

    // 查询库存
    @Override
    public SingleResponse<Integer> queryInventory(InventoryRequest inventoryRequest) {

        GoodsType goodsType = inventoryRequest.getGoodsType();
        // 先从本地缓存中查询是否售罄
        if (soldOutGoodsLocalCache.getIfPresent(goodsType + SEPARATOR + inventoryRequest.getGoodsId()) != null) {
            return SingleResponse.of(0);
        }

        Integer inventory = switch (goodsType) {
            // 先从redis中查询库存
            case COLLECTION -> collectionInventoryRedisService.getInventory(inventoryRequest);
            case BLIND_BOX -> blindBoxInventoryRedisService.getInventory(inventoryRequest);
            case STAR -> starInventoryRedisService.getInventory(inventoryRequest);
            default -> throw new UnsupportedOperationException(ERROR_CODE_UNSUPPORTED_GOODS_TYPE);
        };

        if(inventory == null){
            return SingleResponse.fail(INVENTORY_QUERY_FAILED.getCode(), INVENTORY_QUERY_FAILED.getMessage());
        }
        return SingleResponse.of(inventory);
    }

    @Override
    public SingleResponse<Long> removeInventoryDecreaseLog(InventoryRequest inventoryRequest) {
        GoodsType goodsType = inventoryRequest.getGoodsType();
        Long inventoryResponse = switch (goodsType) {
            case COLLECTION -> collectionInventoryRedisService.removeInventoryDecreaseLog(inventoryRequest);

            case BLIND_BOX -> blindBoxInventoryRedisService.removeInventoryDecreaseLog(inventoryRequest);

            case STAR -> starInventoryRedisService.removeInventoryDecreaseLog(inventoryRequest);

            default -> throw new UnsupportedOperationException(ERROR_CODE_UNSUPPORTED_GOODS_TYPE);
        };

        return SingleResponse.of(inventoryResponse);
    }

    @Override
    public SingleResponse<Long> removeInventoryIncreaseLog(InventoryRequest inventoryRequest) {
        GoodsType goodsType = inventoryRequest.getGoodsType();
        Long inventoryResponse = switch (goodsType) {
            case COLLECTION -> collectionInventoryRedisService.removeInventoryIncreaseLog(inventoryRequest);

            case BLIND_BOX -> blindBoxInventoryRedisService.removeInventoryIncreaseLog(inventoryRequest);

            case STAR -> starInventoryRedisService.removeInventoryIncreaseLog(inventoryRequest);

            default -> throw new UnsupportedOperationException(ERROR_CODE_UNSUPPORTED_GOODS_TYPE);
        };

        return SingleResponse.of(inventoryResponse);
    }

}
