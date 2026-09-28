package cn.kaziki.nft.turbo.collection.domain.service.impl;

import cn.kaziki.nft.turbo.api.collection.constant.CollectionInventoryModifyType;
import cn.kaziki.nft.turbo.api.collection.constant.CollectionStateEnum;
import cn.kaziki.nft.turbo.api.collection.request.*;
import cn.kaziki.nft.turbo.api.goods.request.*;
import cn.kaziki.nft.turbo.collection.domain.entity.*;
import cn.kaziki.nft.turbo.collection.infrastructure.mapper.*;
import cn.yueyu.nft.turbo.api.collection.request.*;
import cn.kaziki.nft.turbo.api.collection.response.CollectionAirdropResponse;
import cn.kaziki.nft.turbo.api.collection.response.CollectionInventoryModifyResponse;
import cn.kaziki.nft.turbo.api.collection.service.SeriesReadFacadeService;
import cn.kaziki.nft.turbo.api.goods.constant.GoodsEvent;
import cn.yueyu.nft.turbo.api.goods.request.*;
import cn.yueyu.nft.turbo.collection.domain.entity.*;
import cn.kaziki.nft.turbo.collection.domain.entity.convertor.CollectionConvertor;
import cn.kaziki.nft.turbo.collection.domain.entity.convertor.HeldCollectionConvertor;
import cn.kaziki.nft.turbo.collection.domain.request.HeldCollectionCreateRequest;
import cn.kaziki.nft.turbo.collection.domain.service.CollectionService;
import cn.kaziki.nft.turbo.collection.exception.CollectionException;
import cn.yueyu.nft.turbo.collection.infrastructure.mapper.*;
import cn.hutool.core.lang.Assert;
import com.alicp.jetcache.anno.CacheInvalidate;
import com.alicp.jetcache.anno.CacheRefresh;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static cn.kaziki.nft.turbo.base.response.ResponseCode.DUPLICATED;
import static cn.kaziki.nft.turbo.base.response.ResponseCode.SUCCESS;
import static cn.kaziki.nft.turbo.collection.exception.CollectionErrorCode.*;

public abstract class BaseCollectionService extends ServiceImpl<CollectionMapper, Collection> implements CollectionService {
    @Autowired
    private CollectionStreamMapper collectionStreamMapper;
    @Autowired
    private CollectionMapper collectionMapper;
    @Autowired
    private CollectionInventoryStreamMapper collectionInventoryStreamMapper;
    @Autowired
    private HeldCollectionService heldCollectionService;
    @Autowired
    private CollectionAirdropStreamMapper collectionAirdropStreamMapper;
    @Autowired
    private CollectionSnapshotMapper collectionSnapshotMapper;
    @Autowired
    private SeriesReadFacadeService seriesReadFacadeService;

    // 创建藏品
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Collection create(CollectionCreateRequest request) {
        // 幂等检查：相同 identifier 已处理过则直接返回已有藏品
        CollectionStream existStream = collectionStreamMapper.selectByIdentifier(request.getIdentifier(), String.valueOf(request.getEventType()), null);
        if (existStream != null) {
            return collectionMapper.selectById(existStream.getCollectionId());
        }
        // 1.创建藏品
        Collection collection = Collection.create(request);
        // 2.存入数据库
        var saveResult = this.save(collection);
        Assert.isTrue(saveResult, () -> new CollectionException(COLLECTION_SAVE_FAILED));
        // 3.创建藏品快照
        CollectionSnapshot collectionSnapshot = CollectionConvertor.INSTANCE.createSnapshot(collection);
        var result = collectionSnapshotMapper.insert(collectionSnapshot);
        Assert.isTrue(result > 0, () -> new CollectionException(COLLECTION_SNAPSHOT_SAVE_FAILED));
        // 4.新增藏品流水
        CollectionStream stream = new CollectionStream(collection, request.getIdentifier(), request.getEventType());
        saveResult = collectionStreamMapper.insert(stream) == 1;
        Assert.isTrue(saveResult, () -> new CollectionException(COLLECTION_STREAM_SAVE_FAILED));
        // 5.新增藏品库存流水
        CollectionInventoryStream inventoryStream = new CollectionInventoryStream(collection, request.getIdentifier(), GoodsEvent.MODIFY_INVENTORY, request.getQuantity());
        saveResult = collectionInventoryStreamMapper.insert(inventoryStream) == 1;
        Assert.isTrue(saveResult, () -> new CollectionException(COLLECTION_INVENTORY_STREAM_SAVE_FAILED));
        return collection;
    }

    // 下架藏品
    @Transactional(rollbackFor = Exception.class)
    @Override
    @CacheInvalidate(name = ":collection:cache:id:", key = "#request.collectionId")
    public Boolean remove(CollectionRemoveRequest request) {
        // 1.查找藏品流水避免是否重复操作
        CollectionStream existStream = collectionStreamMapper.selectByIdentifier(request.getIdentifier(), request.getEventType().name(), request.getCollectionId());
        if (existStream != null) {
            return true;
        }
        // 2.移除藏品
        Collection collection = getById(request.getCollectionId());
        collection.remove();
        var saveResult = this.updateById(collection);
        Assert.isTrue(saveResult, () -> new CollectionException(COLLECTION_UPDATE_FAILED));
        // 3.新增藏品流水
        CollectionStream stream = new CollectionStream(collection, request.getIdentifier(), request.getEventType());
        saveResult = collectionStreamMapper.insert(stream) == 1;
        Assert.isTrue(saveResult, () -> new CollectionException(COLLECTION_STREAM_SAVE_FAILED));
        return true;
    }

    // 修改藏品价格
    @CacheInvalidate(name = ":collection:cache:id:", key = "#request.collectionId")
    @Override
    public Boolean modifyPrice(CollectionModifyPriceRequest request) {
        // 1.查找藏品流水避免是否重复操作
        CollectionStream existStream = collectionStreamMapper.selectByIdentifier(request.getIdentifier(), request.getEventType().name(), request.getCollectionId());
        if (existStream != null) {
            return true;
        }

        // 2.修改藏品版本号和价格
        Collection collection = getById(request.getCollectionId());
        collection.setVersion(collection.getVersion() + 1);
        collection.setPrice(request.getPrice());
        var saveResult = super.updateById(collection);
        Assert.isTrue(saveResult, () -> new CollectionException(COLLECTION_SAVE_FAILED));

        // 3.创建新的藏品快照
        CollectionSnapshot collectionSnapshot = CollectionConvertor.INSTANCE.createSnapshot(collection);
        var result = collectionSnapshotMapper.insert(collectionSnapshot);
        Assert.isTrue(result > 0, () -> new CollectionException(COLLECTION_SNAPSHOT_SAVE_FAILED));

        // 4.新增藏品流水
        CollectionStream stream = new CollectionStream(collection, request.getIdentifier(), request.getEventType());
        saveResult = collectionStreamMapper.insert(stream) == 1;
        Assert.isTrue(saveResult, () -> new CollectionException(COLLECTION_STREAM_SAVE_FAILED));
        return true;
    }

    // 藏品评定（稀有度/系列/铸造值/获取途径）
    @Transactional(rollbackFor = Exception.class)
    @CacheInvalidate(name = ":collection:cache:id:", key = "#request.collectionId")
    @Override
    public Boolean assess(CollectionAssessRequest request) {
        // 1.查找藏品流水避免重复操作
        CollectionStream existStream = collectionStreamMapper.selectByIdentifier(request.getIdentifier(), request.getEventType().name(), request.getCollectionId());
        if (existStream != null) {
            return true;
        }

        // 2.查询藏品 + 评定治理硬校验
        Collection collection = getById(request.getCollectionId());
        if (collection == null) {
            throw new CollectionException(COLLECTION_NOT_EXIST);
        }
        // 2.1 稀有度公示后只升不降（level 比较，不用 ordinal）
        if (collection.getRarity() != null && request.getRarity().getLevel() < collection.getRarity().getLevel()) {
            throw new CollectionException(COLLECTION_RARITY_DOWNGRADE_FORBIDDEN);
        }
        // 2.2 已归入系列不可更换（允许从无到有）
        if (collection.getSeriesId() != null && request.getSeriesId() != null
                && !collection.getSeriesId().equals(request.getSeriesId())) {
            throw new CollectionException(COLLECTION_SERIES_CHANGE_FORBIDDEN);
        }
        // 2.3 目标系列必须存在
        if (request.getSeriesId() != null && seriesReadFacadeService.queryById(request.getSeriesId()) == null) {
            throw new CollectionException(SERIES_NOT_EXIST);
        }

        // 3.更新评定字段和版本号
        collection.setVersion(collection.getVersion() + 1);
        collection.setRarity(request.getRarity());
        if (request.getSeriesId() != null) {
            collection.setSeriesId(request.getSeriesId());
        }
        // 铸造值未指定则按稀有度基准值带出
        collection.setForgeValue(request.getForgeValue() != null ? request.getForgeValue() : request.getRarity().getBaseForgeValue());
        var saveResult = super.updateById(collection);
        Assert.isTrue(saveResult, () -> new CollectionException(COLLECTION_UPDATE_FAILED));

        // 4.创建新的藏品快照
        CollectionSnapshot collectionSnapshot = CollectionConvertor.INSTANCE.createSnapshot(collection);
        var result = collectionSnapshotMapper.insert(collectionSnapshot);
        Assert.isTrue(result > 0, () -> new CollectionException(COLLECTION_SNAPSHOT_SAVE_FAILED));

        // 5.新增藏品流水
        CollectionStream stream = new CollectionStream(collection, request.getIdentifier(), request.getEventType());
        saveResult = collectionStreamMapper.insert(stream) == 1;
        Assert.isTrue(saveResult, () -> new CollectionException(COLLECTION_STREAM_SAVE_FAILED));
        return true;
    }

    // 修改藏品库存
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CollectionInventoryModifyResponse modifyInventory(CollectionModifyInventoryRequest request) {
        CollectionInventoryModifyResponse response = new CollectionInventoryModifyResponse();
        response.setCollectionId(request.getCollectionId());
        // 1.查找藏品流水避免是否重复操作
        CollectionInventoryStream existStream = collectionInventoryStreamMapper.selectByIdentifier(request.getIdentifier(), request.getEventType().name(), request.getCollectionId());
        if (existStream != null) {
            response.setSuccess(true);
            response.setResponseCode(DUPLICATED.name());
            return response;
        }

        // 2.计算变化数量
        Collection collection = getById(request.getCollectionId());
        if (null == collection) {
            throw new CollectionException(COLLECTION_QUERY_FAIL);
        }
        Long quantityDiff = request.getQuantity() - collection.getQuantity();
        response.setQuantityModified(Math.abs(quantityDiff));
        if (quantityDiff == 0) {
            response.setModifyType(CollectionInventoryModifyType.UNMODIFIED);
            response.setSuccess(true);
            return response;
        } else if (quantityDiff > 0) {
            response.setModifyType(CollectionInventoryModifyType.INCREASE);
        } else {
            response.setModifyType(CollectionInventoryModifyType.DECREASE);
        }

        // 3.更新藏品库存
        Long oldSaleableInventory = collection.getSaleableInventory();
        collection.setQuantity(request.getQuantity());
        collection.setSaleableInventory(oldSaleableInventory + quantityDiff);
        boolean res = updateById(collection);

        Assert.isTrue(res, () -> new CollectionException(COLLECTION_UPDATE_FAILED));
        // 4.新增藏品库存流水表
        CollectionInventoryStream inventoryStream = new CollectionInventoryStream(collection, request.getIdentifier(), request.getEventType(), quantityDiff);
        boolean saveResult = collectionInventoryStreamMapper.insert(inventoryStream) == 1;
        Assert.isTrue(saveResult, () -> new CollectionException(COLLECTION_INVENTORY_UPDATE_FAILED));

        response.setSuccess(true);
        return response;
    }

    // 冻结库存
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean freezeInventory(GoodsFreezeInventoryRequest request) {
        // 1.流水校验
        CollectionInventoryStream existStream = collectionInventoryStreamMapper.selectByIdentifier(request.identifier(), request.eventType().name(), request.goodsId());
        if (null != existStream) {
            return true;
        }

        // 2.查询出最新的值
        Collection collection = this.getById(request.goodsId());

        // 3.新增库存流水
        CollectionInventoryStream stream = new CollectionInventoryStream(collection, request.identifier(), request.eventType(), request.quantity());
        int result = collectionInventoryStreamMapper.insert(stream);
        Assert.isTrue(result > 0, () -> new CollectionException(COLLECTION_STREAM_SAVE_FAILED));

        // 4.冻结库存
        result = collectionMapper.freezeInventory(request.goodsId(), request.quantity());
        Assert.isTrue(result == 1, () -> new CollectionException(COLLECTION_SAVE_FAILED));
        return true;
    }

    // 解冻库存
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean unfreezeInventory(GoodsUnfreezeInventoryRequest request) {
        // 1.流水校验
        CollectionInventoryStream existStream = collectionInventoryStreamMapper.selectByIdentifier(request.identifier(), request.eventType().name(), request.goodsId());
        if (null != existStream) {
            return true;
        }

        CollectionInventoryStream existFreezeStream = collectionInventoryStreamMapper.selectByIdentifier(request.identifier(), GoodsEvent.FREEZE_INVENTORY.name(), request.goodsId());
        if (null == existFreezeStream) {
            throw new CollectionException(INVENTORY_UNFREEZE_FAILED);
        }

        // 2.查询出最新的值
        Collection collection = this.getById(request.goodsId());

        // 3.新增库存流水
        CollectionInventoryStream stream = new CollectionInventoryStream(collection, request.identifier(), request.eventType(), request.quantity());
        int result = collectionInventoryStreamMapper.insert(stream);
        Assert.isTrue(result > 0, () -> new CollectionException(COLLECTION_STREAM_SAVE_FAILED));

        // 4.解冻库存
        result = collectionMapper.unfreezeInventory(request.goodsId(), request.quantity());
        Assert.isTrue(result == 1, () -> new CollectionException(COLLECTION_SAVE_FAILED));
        return true;
    }

    // 解冻库存并售卖
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean unfreezeAndSale(GoodsUnfreezeAndSaleRequest request) {
        // 1.流水校验
        CollectionInventoryStream existStream = collectionInventoryStreamMapper.selectByIdentifier(request.identifier(), request.eventType().name(), request.goodsId());
        if (null != existStream) {
            return true;
        }

        CollectionInventoryStream existFreezeStream = collectionInventoryStreamMapper.selectByIdentifier(request.identifier(), GoodsEvent.FREEZE_INVENTORY.name(), request.goodsId());
        if (null == existFreezeStream) {
            throw new CollectionException(INVENTORY_UNFREEZE_FAILED);
        }

        // 2.查询出最新的值
        Collection collection = this.getById(request.goodsId());

        // 3.新增库存流水
        CollectionInventoryStream stream = new CollectionInventoryStream(collection, request.identifier(), request.eventType(), request.quantity());
        int result = collectionInventoryStreamMapper.insert(stream);
        Assert.isTrue(result > 0, () -> new CollectionException(COLLECTION_STREAM_SAVE_FAILED));

        // 4.解冻并扣减库存
        result = collectionMapper.unfreezeAndSale(request.goodsId(), request.quantity());
        Assert.isTrue(result == 1, () -> new CollectionException(COLLECTION_SAVE_FAILED));
        return true;
    }


    // 根据id查询藏品
    @Override
    @Cached(name = ":collection:cache:id:", expire = 60, localExpire = 10, timeUnit = TimeUnit.MINUTES, cacheType = CacheType.BOTH,key = "#collectionId", cacheNullValue = true)
    @CacheRefresh(refresh = 50, timeUnit = TimeUnit.MINUTES)
    public Collection queryById(Long collectionId) {
        return getById(collectionId);
    }

    // 驳回藏品（只更新状态，不写流水/快照）
    @Override
    @CacheInvalidate(name = ":collection:cache:id:", key = "#request.collectionId")
    public Boolean rejectCollection(CollectionStateChangeRequest request) {
        Collection collection = getById(request.getCollectionId());
        if (collection == null) {
            throw new CollectionException(COLLECTION_NOT_EXIST);
        }

        collection.setState(request.getTargetState());
        collection.setVersion(collection.getVersion() + 1);
        var saveResult = super.updateById(collection);
        Assert.isTrue(saveResult, () -> new CollectionException(COLLECTION_UPDATE_FAILED));
        return true;
    }

    // 重新提交审核（INIT → PENDING_REVIEW，不写流水/快照）
    @Override
    @CacheInvalidate(name = ":collection:cache:id:", key = "#request.collectionId")
    public Boolean resubmitCollection(CollectionStateChangeRequest request) {
        Collection collection = getById(request.getCollectionId());
        if (collection == null) {
            throw new CollectionException(COLLECTION_NOT_EXIST);
        }
        collection.setState(CollectionStateEnum.PENDING_REVIEW);
        collection.setVersion(collection.getVersion() + 1);
        var saveResult = super.updateById(collection);
        Assert.isTrue(saveResult, () -> new CollectionException(COLLECTION_UPDATE_FAILED));
        return true;
    }

    // 取消订单支付--库存退还
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean cancel(GoodsCancelSaleRequest request) {
        CollectionInventoryStream existStream = collectionInventoryStreamMapper.selectByIdentifier(request.identifier(),request.eventType().name(),request.goodsId());
        if(null != existStream){
            return true;
        }

        Collection collection = getById(request.goodsId());
        CollectionInventoryStream inventoryStream = new CollectionInventoryStream(collection,request.identifier(),request.eventType(),request.quantity());
        inventoryStream.setExtendInfo(request.extendInfo());
        int result = collectionInventoryStreamMapper.insert(inventoryStream);
        Assert.isTrue(result == 1, () -> new CollectionException(COLLECTION_STREAM_SAVE_FAILED));

        result = collectionMapper.cancel(request.goodsId(), request.quantity());
        Assert.isTrue(result == 1, () -> new CollectionException(COLLECTION_SAVE_FAILED));

        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean sale(GoodsTrySaleRequest request) {
        // 1.流水校验
        CollectionInventoryStream existStream = collectionInventoryStreamMapper.selectByIdentifier(request.identifier(), request.eventType().name(), request.goodsId());
        if (null != existStream) {
            return true;
        }

        // 2.查询出最新的值
        Collection collection = this.getById(request.goodsId());

        // 3.新增collection流水
        CollectionInventoryStream stream = new CollectionInventoryStream(collection, request.identifier(), request.eventType(), request.quantity());
        int result = collectionInventoryStreamMapper.insert(stream);
        Assert.isTrue(result > 0, () -> new CollectionException(COLLECTION_STREAM_SAVE_FAILED));

        // 4.核心逻辑执行，更新藏品可售库存
        result = collectionMapper.sale(request.goodsId(), request.quantity());
        Assert.isTrue(result == 1, () -> new CollectionException(COLLECTION_SAVE_FAILED));
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean saleWithoutHint(GoodsTrySaleRequest goodsTrySaleRequest) {
        // 1.流水校验
        CollectionInventoryStream existStream = collectionInventoryStreamMapper.selectByIdentifier(goodsTrySaleRequest.identifier(), goodsTrySaleRequest.eventType().name(), goodsTrySaleRequest.goodsId());
        if (null != existStream) {
            return true;
        }

        // 2.查询出最新的值
        Collection collection = this.getById(goodsTrySaleRequest.goodsId());

        // 3.新增藏品库存流水
        CollectionInventoryStream stream = new CollectionInventoryStream(collection, goodsTrySaleRequest.identifier(), goodsTrySaleRequest.eventType(), goodsTrySaleRequest.quantity());
        stream.setExtendInfo(goodsTrySaleRequest.extendInfo());
        int result = collectionInventoryStreamMapper.insert(stream);
        Assert.isTrue(result > 0, () -> new CollectionException(COLLECTION_STREAM_SAVE_FAILED));

        // 4.扣减藏品库存
        result = collectionMapper.saleWithoutHint(goodsTrySaleRequest.goodsId(), goodsTrySaleRequest.quantity());
        Assert.isTrue(result == 1, () -> new CollectionException(COLLECTION_SAVE_FAILED));
        return true;
    }

    @SuppressWarnings("AliDeprecation")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public CollectionAirdropResponse airDrop(CollectionAirDropRequest request, Collection collection) {
        CollectionAirdropStream existStream = collectionAirdropStreamMapper.selectByIdentifier(request.getIdentifier(), request.getEventType().name(), request.getCollectionId(), request.getRecipientUserId());
        CollectionAirdropResponse response = new CollectionAirdropResponse();
        if (existStream != null) {
            response.setSuccess(true);
            response.setResponseCode(DUPLICATED.name());
            response.setAirDropStreamId(existStream.getId());
            return response;
        }

        //新增流水
        CollectionInventoryStream stream = new CollectionInventoryStream(collection, request.getIdentifier(), request.getEventType(), request.getQuantity());
        int result = collectionInventoryStreamMapper.insert(stream);
        Assert.isTrue(result > 0, () -> new CollectionException(COLLECTION_STREAM_SAVE_FAILED));

        //新增空投流水
        CollectionAirdropStream airdropStream = new CollectionAirdropStream(collection, request.getIdentifier(), request.getEventType(), request.getQuantity(), request.getRecipientUserId());
        boolean saveResult = collectionAirdropStreamMapper.insert(airdropStream) == 1;
        Assert.isTrue(saveResult, () -> new CollectionException(COLLECTION_AIRDROP_STREAM_UPDATE_FAILED));

        List<HeldCollectionCreateRequest> heldCollectionCreateRequests = new ArrayList<>();
        for (int i = 1; i <= request.getQuantity(); i++) {

            HeldCollectionCreateRequest heldCollectionCreateRequest = new HeldCollectionCreateRequest(request, collection, airdropStream);
            heldCollectionCreateRequest.setSerialNoBaseId(request.getCollectionId().toString());

            heldCollectionCreateRequests.add(heldCollectionCreateRequest);
        }

        List<HeldCollection> heldCollections = heldCollectionService.batchCreate(heldCollectionCreateRequests);

        //扣减藏品库存
        result = collectionMapper.airDrop(request.getCollectionId(), request.getQuantity());
        Assert.isTrue(result == 1, () -> new CollectionException(COLLECTION_SAVE_FAILED));

        response.setSuccess(true);
        response.setResponseCode(SUCCESS.name());
        response.setAirDropStreamId(airdropStream.getId());
        response.setHeldCollections(HeldCollectionConvertor.INSTANCE.mapToVo(heldCollections));
        return response;
    }
}
