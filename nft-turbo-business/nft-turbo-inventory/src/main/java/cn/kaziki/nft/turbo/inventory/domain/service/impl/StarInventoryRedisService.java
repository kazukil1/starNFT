package cn.kaziki.nft.turbo.inventory.domain.service.impl;

import cn.kaziki.nft.turbo.api.inventory.request.InventoryRequest;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 星尘闪购库存 — Redis 服务
 * Key 带日期后缀，每日自动切换，TTL 由 XXL-Job init 时设定
 */
@Service
public class StarInventoryRedisService extends AbstractInventoryRedisService {

    private static final String INVENTORY_KEY_PREFIX = "star:daily:";
    private static final String INVENTORY_STREAM_KEY_PREFIX = "star:daily:stream:";
    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyyMMdd"));

    private String todaySuffix() {
        return DATE_FORMAT.get().format(new Date());
    }

    @Override
    protected String getCacheKey(InventoryRequest request) {
        return INVENTORY_KEY_PREFIX + request.getGoodsId() + ":" + todaySuffix();
    }

    @Override
    protected String getCacheStreamKey(InventoryRequest request) {
        return INVENTORY_STREAM_KEY_PREFIX + request.getGoodsId() + ":" + todaySuffix();
    }
}
