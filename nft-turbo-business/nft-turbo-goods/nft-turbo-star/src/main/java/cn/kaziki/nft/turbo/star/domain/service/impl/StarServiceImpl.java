package cn.kaziki.nft.turbo.star.domain.service.impl;

import cn.kaziki.nft.turbo.api.goods.constant.GoodsEvent;
import cn.kaziki.nft.turbo.api.goods.request.GoodsCancelSaleRequest;
import cn.kaziki.nft.turbo.api.goods.request.GoodsTrySaleRequest;
import cn.kaziki.nft.turbo.api.star.constant.StarChangeType;
import cn.kaziki.nft.turbo.api.star.constant.StarState;
import cn.kaziki.nft.turbo.api.star.request.StarChangeRequest;
import cn.kaziki.nft.turbo.api.star.request.StarCreateRequest;
import cn.kaziki.nft.turbo.api.star.request.StarModifyInventoryRequest;
import cn.kaziki.nft.turbo.api.star.request.StarModifyRequest;
import cn.kaziki.nft.turbo.api.star.request.StarPageQueryRequest;
import cn.kaziki.nft.turbo.api.star.request.StarRemoveRequest;
import cn.kaziki.nft.turbo.base.response.PageResponse;
import cn.kaziki.nft.turbo.star.domain.entity.StarDaily;
import cn.kaziki.nft.turbo.star.domain.entity.StarInventoryStream;
import cn.kaziki.nft.turbo.star.domain.service.StarAccountService;
import cn.kaziki.nft.turbo.star.domain.service.StarService;
import cn.kaziki.nft.turbo.star.exception.StarErrorCode;
import cn.kaziki.nft.turbo.star.exception.StarException;
import cn.kaziki.nft.turbo.star.infrastructure.mapper.StarDailyMapper;
import cn.kaziki.nft.turbo.star.infrastructure.mapper.StarInventoryStreamMapper;
import cn.hutool.core.lang.Assert;
import com.alicp.jetcache.anno.CacheInvalidate;
import com.alicp.jetcache.anno.CacheRefresh;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 星尘领域服务实现
 */
@Slf4j
@Service
public class StarServiceImpl extends ServiceImpl<StarDailyMapper, StarDaily> implements StarService {

    @Autowired
    private StarInventoryStreamMapper starInventoryStreamMapper;

    @Autowired
    private StarAccountService starAccountService;

    private Cache<String, Boolean> soldOutLocalCache;

    @PostConstruct
    public void init() {
        soldOutLocalCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(100)
                .build();
    }

    // ==================== 读取 ====================

    @Override
    public PageResponse<StarDaily> pageQuery(StarPageQueryRequest request) {
        Page<StarDaily> page = new Page<>(request.getCurrentPage(), request.getPageSize());
        QueryWrapper<StarDaily> wrapper = new QueryWrapper<>();
        if (request.getState() != null && !request.getState().isEmpty()) {
            wrapper.eq("state", request.getState());
        } else {
            wrapper.eq("state", StarState.ACTIVE.name());
        }
        if (request.getKeyword() != null && !request.getKeyword().isEmpty()) {
            wrapper.like("name", request.getKeyword());
        }
        wrapper.orderByAsc("id");
        Page<StarDaily> result = page(page, wrapper);
        return PageResponse.of(result.getRecords(), (int) result.getTotal(),
                request.getPageSize(), request.getCurrentPage());
    }

    @Override
    @Cached(name = ":star:daily:id:", expire = 60, localExpire = 10, timeUnit = TimeUnit.MINUTES,
            cacheType = CacheType.BOTH, key = "#id", cacheNullValue = true)
    @CacheRefresh(refresh = 50, timeUnit = TimeUnit.MINUTES)
    public StarDaily queryById(Long id) {
        return getById(id);
    }

    // ==================== 写入（流水模式） ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean sale(GoodsTrySaleRequest request) {
        StarInventoryStream exist = starInventoryStreamMapper
                .selectByIdentifier(request.identifier(), GoodsEvent.TRY_SALE.name(), request.goodsId());
        if (exist != null) {
            return true;
        }

        String cacheKey = "STAR:" + request.goodsId();
        if (soldOutLocalCache.getIfPresent(cacheKey) != null) {
            throw new StarException(StarErrorCode.STAR_SOLD_OUT);
        }

        int result = baseMapper.sale(request.goodsId(), (long) request.quantity());
        if (result != 1) {
            soldOutLocalCache.put(cacheKey, true);
            throw new StarException(StarErrorCode.STAR_SOLD_OUT);
        }

        writeInventoryStream(request.identifier(), request.goodsId(), (long) request.quantity(),
                GoodsEvent.TRY_SALE);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean saleWithoutHint(GoodsTrySaleRequest request) {
        StarInventoryStream exist = starInventoryStreamMapper
                .selectByIdentifier(request.identifier(), GoodsEvent.TRY_SALE.name(), request.goodsId());
        if (exist != null) {
            return true;
        }

        int result = baseMapper.sale(request.goodsId(), (long) request.quantity());
        if (result != 1) {
            throw new StarException(StarErrorCode.STAR_SOLD_OUT);
        }

        writeInventoryStream(request.identifier(), request.goodsId(), (long) request.quantity(),
                GoodsEvent.TRY_SALE);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean cancel(GoodsCancelSaleRequest request) {
        StarInventoryStream exist = starInventoryStreamMapper
                .selectByIdentifier(request.identifier(), GoodsEvent.CANCEL_SALE.name(), request.goodsId());
        if (exist != null) {
            return true;
        }

        StarDaily star = super.getById(request.goodsId());
        if (star == null) {
            throw new StarException(StarErrorCode.STAR_NOT_EXIST);
        }
        int result = baseMapper.cancel(request.goodsId(), request.quantity());
        if (result != 1) {
            throw new StarException(StarErrorCode.STAR_CANCEL_FAILED);
        }
        soldOutLocalCache.invalidate("STAR:" + request.goodsId());

        writeInventoryStream(request.identifier(), request.goodsId(), request.quantity(),
                GoodsEvent.CANCEL_SALE);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void paySuccess(Long starId, String userId, Long quantity, String orderId) {
        StarDaily star = super.getById(starId);
        if (star == null) {
            throw new StarException(StarErrorCode.STAR_NOT_EXIST);
        }

        // 库存已在 sale 时扣减，此处只写账户流水（increase 自带幂等）
        StarChangeRequest changeRequest = new StarChangeRequest();
        changeRequest.setUserId(userId);
        changeRequest.setAmount(safeStarAmount(star, quantity));
        changeRequest.setChangeType(StarChangeType.PURCHASE);
        changeRequest.setBizNo(orderId);
        changeRequest.setBizType("STAR_PURCHASE");
        changeRequest.setIdentifier("PAY_" + orderId);
        starAccountService.increase(changeRequest);
    }

    // 溢出安全：starAmount * quantity 可能超过 Long.MAX_VALUE
    private Long safeStarAmount(StarDaily star, Long quantity) {
        return Math.multiplyExact(star.getStarAmount(), quantity);
    }

    private void writeInventoryStream(String identifier, Long starId, Long changedQuantity,
                                       GoodsEvent streamType) {
        StarDaily star = getById(starId);
        StarInventoryStream stream = new StarInventoryStream(star, identifier, streamType, changedQuantity);
        boolean result = starInventoryStreamMapper.insert(stream) > 0;
        Assert.isTrue(result, () -> new StarException(StarErrorCode.STAR_STREAM_SAVE_FAILED));
    }

    // ==================== 管理端 CRUD ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(StarCreateRequest request) {
        StarInventoryStream exist = starInventoryStreamMapper
                .selectByIdentifier(request.getIdentifier(), GoodsEvent.CREATE.name(), null);
        if (exist != null) {
            return exist.getStarId();
        }

        StarDaily star = StarDaily.create(request);
        boolean result = save(star);
        Assert.isTrue(result, () -> new StarException(StarErrorCode.ACCOUNT_SAVE_FAILED));

        writeInventoryStream(request.getIdentifier(), star.getId(), star.getQuantity(),
                GoodsEvent.CREATE);

        return star.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheInvalidate(name = ":star:daily:id:", key = "#request.id")
    public Long modify(StarModifyRequest request) {
        StarDaily star = getById(request.getId());
        if (star == null) {
            throw new StarException(StarErrorCode.STAR_NOT_EXIST);
        }

        if (request.getName() != null) star.setName(request.getName());
        if (request.getCover() != null) star.setCover(request.getCover());
        if (request.getStarAmount() != null) star.setStarAmount(request.getStarAmount());
        if (request.getPrice() != null) star.setPrice(request.getPrice());
        if (request.getDetail() != null) star.setDetail(request.getDetail());
        if (request.getSaleTime() != null) star.setSaleTime(request.getSaleTime());

        boolean result = updateById(star);
        Assert.isTrue(result, () -> new StarException(StarErrorCode.ACCOUNT_SAVE_FAILED));

        return star.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheInvalidate(name = ":star:daily:id:", key = "#request.id")
    public Long modifyInventory(StarModifyInventoryRequest request) {
        GoodsEvent streamType = GoodsEvent.MODIFY_INVENTORY;
        StarInventoryStream exist = starInventoryStreamMapper
                .selectByIdentifier(request.getIdentifier(), streamType.name(), request.getId());
        if (exist != null) {
            return request.getId();
        }

        StarDaily star = getById(request.getId());
        if (star == null) {
            throw new StarException(StarErrorCode.STAR_NOT_EXIST);
        }

        long newInventory = star.getSaleableInventory() + request.getQuantityDelta();
        if (newInventory < 0) {
            throw new StarException(StarErrorCode.STAR_SOLD_OUT);
        }
        star.setSaleableInventory(newInventory);
        star.setQuantity(star.getQuantity() + request.getQuantityDelta());

        boolean result = updateById(star);
        Assert.isTrue(result, () -> new StarException(StarErrorCode.ACCOUNT_SAVE_FAILED));

        writeInventoryStream(request.getIdentifier(), star.getId(),
                request.getQuantityDelta(), streamType);

        return star.getId();
    }

    // ==================== 定时任务 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheInvalidate(name = ":star:daily:id:", key = "#request.id")
    public Boolean remove(StarRemoveRequest request) {
        StarDaily star = getById(request.getId());
        if (star == null) {
            throw new StarException(StarErrorCode.STAR_NOT_EXIST);
        }
        star.remove();
        boolean result = updateById(star);
        Assert.isTrue(result, () -> new StarException(StarErrorCode.ACCOUNT_SAVE_FAILED));
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<StarDaily> initTodayInventory() {
        QueryWrapper<StarDaily> wrapper = new QueryWrapper<>();
        wrapper.eq("state", StarState.ACTIVE.name());
        List<StarDaily> activeList = list(wrapper);

        for (StarDaily star : activeList) {
            baseMapper.resetInventory(star.getId());
            String identifier = "DAILY_RESET_" + star.getId() + "_" + System.currentTimeMillis();
            writeInventoryStream(identifier, star.getId(), star.getQuantity(),
                    GoodsEvent.MODIFY_INVENTORY);
        }

        soldOutLocalCache.invalidateAll();
        log.info("星尘库存初始化完成，共 {} 个 ACTIVE 包", activeList.size());
        return activeList;
    }
}
