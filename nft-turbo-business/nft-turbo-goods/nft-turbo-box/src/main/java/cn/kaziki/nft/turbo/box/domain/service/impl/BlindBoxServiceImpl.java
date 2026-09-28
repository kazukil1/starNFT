package cn.kaziki.nft.turbo.box.domain.service.impl;

import cn.kaziki.nft.turbo.api.box.constant.BlindAllotBoxRule;
import cn.kaziki.nft.turbo.api.box.request.BlindBoxCreateRequest;
import cn.kaziki.nft.turbo.api.box.request.BlindBoxItemCreateRequest;
import cn.kaziki.nft.turbo.api.box.request.BlindBoxModifyInventoryRequest;
import cn.kaziki.nft.turbo.api.goods.constant.GoodsEvent;
import cn.kaziki.nft.turbo.api.goods.request.*;
import cn.yueyu.nft.turbo.api.goods.request.*;
import cn.kaziki.nft.turbo.base.response.PageResponse;
import cn.kaziki.nft.turbo.box.domain.entity.BlindBox;
import cn.kaziki.nft.turbo.box.domain.entity.BlindBoxInventoryStream;
import cn.kaziki.nft.turbo.box.domain.entity.BlindBoxItem;
import cn.kaziki.nft.turbo.box.domain.request.BlindBoxAssignRequest;
import cn.kaziki.nft.turbo.box.domain.request.BlindBoxBindMatchRequest;
import cn.kaziki.nft.turbo.box.domain.service.BlindBoxItemService;
import cn.kaziki.nft.turbo.box.domain.service.BlindBoxRuleServiceFactory;
import cn.kaziki.nft.turbo.box.domain.service.BlindBoxService;
import cn.kaziki.nft.turbo.box.exception.BlindBoxException;
import cn.kaziki.nft.turbo.box.infrastructure.mapper.BlindBoxInventoryStreamMapper;
import cn.kaziki.nft.turbo.box.infrastructure.mapper.BlindBoxMapper;
import cn.kaziki.nft.turbo.lock.DistributeLock;

import cn.hutool.core.lang.Assert;
import com.alicp.jetcache.anno.CacheInvalidate;
import com.alicp.jetcache.anno.CacheRefresh;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static cn.kaziki.nft.turbo.box.exception.BlindBoxErrorCode.*;

/**
 * 通用的盲盒服务
 */
@Service
public class BlindBoxServiceImpl extends ServiceImpl<BlindBoxMapper, BlindBox> implements BlindBoxService {

    @Autowired
    private BlindBoxInventoryStreamMapper blindBoxInventoryStreamMapper;
    @Autowired
    private BlindBoxItemService blindBoxItemService;
    @Autowired
    private BlindBoxMapper blindBoxMapper;
    @Autowired
    private BlindBoxRuleServiceFactory blindBoxRuleServiceFactory;

    @Override
    public PageResponse<BlindBox> pageQueryByState(String keyword, String state, String creatorId, Long seriesId, int currentPage, int pageSize) {
        Page<BlindBox> page = new Page<>(currentPage, pageSize);
        QueryWrapper<BlindBox> wrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(state)) {
            wrapper.eq("state", state);
        }
        wrapper.like(StringUtils.isNotBlank(keyword),"name", keyword);
        if (StringUtils.isNotBlank(creatorId)) {
            wrapper.eq("creator_id", creatorId);
        }
        if (seriesId != null) {
            wrapper.eq("series_id", seriesId);
        }
        wrapper.orderBy(true, true, "gmt_create");
        Page<BlindBox> blindBoxPage = page(page, wrapper);
        return PageResponse.of(blindBoxPage.getRecords(), (int) blindBoxPage.getTotal(), pageSize, currentPage);
    }

    // 创建盲盒
    @Transactional(rollbackFor = Exception.class)
    @Override
    public BlindBox create(BlindBoxCreateRequest request) {
        // 1.创建盲盒
        BlindBox blindBox = BlindBox.create(request);
        var saveResult = this.save(blindBox);
        Assert.isTrue(saveResult, () -> new BlindBoxException(BLIND_BOX_SAVE_FAILED));

        // 2.构造盲盒条目
        List<BlindBoxItem> items = new ArrayList<>();
        for (BlindBoxItemCreateRequest boxItemCreateRequest : request.getBlindBoxItemCreateRequests()) {
            Long quality = boxItemCreateRequest.getQuantity();
            // 数量有多少个，就循环初始化多少个盲盒条目
            IntStream.range(0, quality.intValue()).forEach(i -> {
                BlindBoxItem blindBoxItem = BlindBoxItem.create(boxItemCreateRequest, blindBox);
                items.add(blindBoxItem);
            });
        }

        // 3.批量插入数据库
        saveResult = blindBoxItemService.batchCreateItem(items);
        Assert.isTrue(saveResult, () -> new BlindBoxException(BLIND_BOX_ITEM_SAVE_FAILED));

        return blindBox;
    }

    // 补充库存（流水模式：幂等→流水→核心操作）
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long modifyInventory(BlindBoxModifyInventoryRequest request) {
        // 1. 幂等校验
        BlindBoxInventoryStream existStream = blindBoxInventoryStreamMapper
                .selectByIdentifier(request.getIdentifier(), request.getEventType().name(), request.getBlindBoxId());
        if (null != existStream) {
            return request.getBlindBoxId();
        }

        // 2. 查询盲盒
        BlindBox blindBox = this.getById(request.getBlindBoxId());
        Assert.notNull(blindBox, () -> new BlindBoxException(BLIND_BOX_NOT_EXIST));

        // 3. 计算增量
        long additionalQuantity = 0;
        for (BlindBoxItemCreateRequest itemRequest : request.getItems()) {
            additionalQuantity += itemRequest.getQuantity();
        }

        // 4. 写流水
        BlindBoxInventoryStream stream = new BlindBoxInventoryStream(blindBox, request.getIdentifier(),
                request.getEventType(), additionalQuantity);
        int result = blindBoxInventoryStreamMapper.insert(stream);
        Assert.isTrue(result > 0, () -> new BlindBoxException(BLIND_BOX_STREAM_SAVE_FAILED));

        // 5. 展开生成 BlindBoxItem
        for (BlindBoxItemCreateRequest itemRequest : request.getItems()) {
            long qty = itemRequest.getQuantity();
            for (int i = 0; i < qty; i++) {
                BlindBoxItem item = BlindBoxItem.create(itemRequest, blindBox);
                blindBoxItemService.save(item);
            }
        }

        // 6. 更新盲盒库存
        blindBox.setQuantity(blindBox.getQuantity() + additionalQuantity);
        blindBox.setSaleableInventory(blindBox.getSaleableInventory() + additionalQuantity);
        this.updateById(blindBox);

        return blindBox.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean sale(GoodsTrySaleRequest request) {
        //流水校验
        BlindBoxInventoryStream existStream = blindBoxInventoryStreamMapper.selectByIdentifier(request.identifier(), request.eventType().name(), request.goodsId());
        if (null != existStream) {
            return true;
        }

        //查询出最新的值
        BlindBox blindBox = this.getById(request.goodsId());

        //新增blindBox流水
        BlindBoxInventoryStream stream = new BlindBoxInventoryStream(blindBox, request.identifier(), request.eventType(), request.quantity());
        int result = blindBoxInventoryStreamMapper.insert(stream);
        Assert.isTrue(result > 0, () -> new BlindBoxException(BLIND_BOX_STREAM_SAVE_FAILED));

        //核心逻辑执行
        result = blindBoxMapper.sale(request.goodsId(), request.quantity());
        Assert.isTrue(result == 1, () -> new BlindBoxException(BLIND_BOX_SAVE_FAILED));
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean saleWithoutHint(GoodsTrySaleRequest request) {
        // 1.流水校验
        BlindBoxInventoryStream existStream = blindBoxInventoryStreamMapper.selectByIdentifier(request.identifier(), request.eventType().name(), request.goodsId());
        if (null != existStream) {
            return true;
        }

        // 2.查询出最新的值
        BlindBox blindBox = this.getById(request.goodsId());

        // 3.新增盲盒流水
        BlindBoxInventoryStream stream = new BlindBoxInventoryStream(blindBox, request.identifier(), request.eventType(), request.quantity());
        int result = blindBoxInventoryStreamMapper.insert(stream);
        Assert.isTrue(result > 0, () -> new BlindBoxException(BLIND_BOX_STREAM_SAVE_FAILED));

        // 4.更新盲盒表
        result = blindBoxMapper.saleWithoutHint(request.goodsId(), request.quantity());
        Assert.isTrue(result == 1, () -> new BlindBoxException(BLIND_BOX_SAVE_FAILED));
        return true;
    }

    // 冻结库存
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean freezeInventory(GoodsFreezeInventoryRequest request) {
        // 1.流水校验
        BlindBoxInventoryStream existStream = blindBoxInventoryStreamMapper.selectByIdentifier(request.identifier(), request.eventType().name(), request.goodsId());
        if (null != existStream) {
            return true;
        }

        // 2.查询出最新的值
        BlindBox blindBox = this.getById(request.goodsId());

        // 3.新增blindBox流水
        BlindBoxInventoryStream stream = new BlindBoxInventoryStream(blindBox, request.identifier(), request.eventType(), request.quantity());
        int result = blindBoxInventoryStreamMapper.insert(stream);
        Assert.isTrue(result > 0, () -> new BlindBoxException(BLIND_BOX_STREAM_SAVE_FAILED));

        // 4.冻结库存
        result = blindBoxMapper.freezeInventory(request.goodsId(), request.quantity());
        Assert.isTrue(result == 1, () -> new BlindBoxException(BLIND_BOX_SAVE_FAILED));
        return true;
    }

    // 解冻库存并售卖
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean unfreezeAndSale(GoodsUnfreezeAndSaleRequest request) {
        // 1.流水校验
        BlindBoxInventoryStream existStream = blindBoxInventoryStreamMapper.selectByIdentifier(request.identifier(), request.eventType().name(), request.goodsId());
        if (null != existStream) {
            return true;
        }

        BlindBoxInventoryStream existFreezeStream = blindBoxInventoryStreamMapper.selectByIdentifier(request.identifier(), GoodsEvent.FREEZE_INVENTORY.name(), request.goodsId());
        if (null == existFreezeStream) {
            throw new BlindBoxException(INVENTORY_UNFREEZE_FAILED);
        }

        // 2.查询出最新的值
        BlindBox blindBox = this.getById(request.goodsId());

        // 3.新增blindBox流水
        BlindBoxInventoryStream stream = new BlindBoxInventoryStream(blindBox, request.identifier(), request.eventType(), request.quantity());
        int result = blindBoxInventoryStreamMapper.insert(stream);
        Assert.isTrue(result > 0, () -> new BlindBoxException(BLIND_BOX_STREAM_SAVE_FAILED));

        // 4.解冻并扣减库存
        result = blindBoxMapper.unfreezeAndSale(request.goodsId(), request.quantity());
        Assert.isTrue(result == 1, () -> new BlindBoxException(BLIND_BOX_SAVE_FAILED));
        return true;
    }

    // 解冻库存
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean unfreezeInventory(GoodsUnfreezeInventoryRequest request) {
        // 1.流水校验
        BlindBoxInventoryStream existStream = blindBoxInventoryStreamMapper.selectByIdentifier(request.identifier(), request.eventType().name(), request.goodsId());
        if (null != existStream) {
            return true;
        }

        BlindBoxInventoryStream existFreezeStream = blindBoxInventoryStreamMapper.selectByIdentifier(request.identifier(), GoodsEvent.FREEZE_INVENTORY.name(), request.goodsId());
        if (null == existFreezeStream) {
            throw new BlindBoxException(INVENTORY_UNFREEZE_FAILED);
        }

        // 2.查询出最新的值
        BlindBox blindBox = this.getById(request.goodsId());

        // 3.新增blindBox流水
        BlindBoxInventoryStream stream = new BlindBoxInventoryStream(blindBox, request.identifier(), request.eventType(), request.quantity());
        int result = blindBoxInventoryStreamMapper.insert(stream);
        Assert.isTrue(result > 0, () -> new BlindBoxException(BLIND_BOX_STREAM_SAVE_FAILED));

        // 4.解冻库存
        result = blindBoxMapper.unfreezeInventory(request.goodsId(), request.quantity());
        Assert.isTrue(result == 1, () -> new BlindBoxException(BLIND_BOX_SAVE_FAILED));
        return true;
    }

    @Override
    @DistributeLock(keyExpression = "#request.blindBoxId", scene = "BLIND_BOX_ASSIGN")
    public Boolean assign(BlindBoxAssignRequest request) {
        BlindBox blindBox = this.getById(request.getBlindBoxId());
        // 1.调用规则分配盲盒条目
        BlindAllotBoxRule ruleName = blindBox.getAllocateRule();
        BlindBoxBindMatchRequest matchRequest = new BlindBoxBindMatchRequest();
        matchRequest.setBlindBoxId(request.getBlindBoxId());
        Long blindBoxItemId = blindBoxRuleServiceFactory.get(ruleName).match(matchRequest);
        Assert.notNull(blindBoxItemId, () -> new BlindBoxException(BLIND_BOX_ITEM_ALLOCATE_FAILED));

        // 2.更新blindBoxItem状态
        BlindBoxItem blindBoxItem = new BlindBoxItem();
        blindBoxItem.setId(blindBoxItemId);
        blindBoxItem.assign(request);
        boolean updateResult = blindBoxItemService.updateById(blindBoxItem);
        Assert.isTrue(updateResult, () -> new BlindBoxException(BLIND_BOX_UPDATE_FAILED));
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean cancel(GoodsCancelSaleRequest request) {
        //流水校验
        BlindBoxInventoryStream existStream = blindBoxInventoryStreamMapper.selectByIdentifier(request.identifier(), request.eventType().name(), request.goodsId());
        if (null != existStream) {
            return true;
        }

        //查询出最新的值
        BlindBox blindBox = this.getById(request.goodsId());

        //新增collection流水
        BlindBoxInventoryStream stream = new BlindBoxInventoryStream(blindBox, request.identifier(), request.eventType(), request.quantity());
        int result = blindBoxInventoryStreamMapper.insert(stream);
        Assert.isTrue(result > 0, () -> new BlindBoxException(BLIND_BOX_STREAM_SAVE_FAILED));

        //核心逻辑执行
        result = blindBoxMapper.cancel(request.goodsId(), request.quantity());
        Assert.isTrue(result == 1, () -> new BlindBoxException(BLIND_BOX_SAVE_FAILED));
        return true;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheInvalidate(name = ":blindBox:cache:id:", key = "#blindBox.id")
    public boolean updateById(BlindBox blindBox) {
        var saveResult = super.updateById(blindBox);
        Assert.isTrue(saveResult, () -> new BlindBoxException(BLIND_BOX_SAVE_FAILED));
        return true;
    }


    @Override
    @Cached(name = ":blindBox:cache:id:", expire = 60, localExpire = 10, timeUnit = TimeUnit.MINUTES, cacheType = CacheType.BOTH, key = "#blindBoxId", cacheNullValue = true)
    @CacheRefresh(refresh = 50, timeUnit = TimeUnit.MINUTES)
    public BlindBox queryById(Long blindBoxId) {
        return this.getById(blindBoxId);
    }

}
