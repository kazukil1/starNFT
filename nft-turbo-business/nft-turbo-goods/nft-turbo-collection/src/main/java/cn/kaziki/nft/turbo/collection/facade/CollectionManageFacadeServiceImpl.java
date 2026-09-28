package cn.kaziki.nft.turbo.collection.facade;

import cn.kaziki.nft.turbo.api.chain.constant.ChainOperateBizTypeEnum;
import cn.kaziki.nft.turbo.api.chain.request.ChainProcessRequest;
import cn.kaziki.nft.turbo.api.chain.response.ChainProcessResponse;
import cn.kaziki.nft.turbo.api.chain.response.data.ChainOperationData;
import cn.kaziki.nft.turbo.api.chain.service.ChainFacadeService;
import cn.kaziki.nft.turbo.api.collection.constant.CollectionInventoryModifyType;
import cn.kaziki.nft.turbo.api.collection.constant.HeldCollectionState;
import cn.kaziki.nft.turbo.api.collection.model.HeldCollectionVO;
import cn.kaziki.nft.turbo.api.collection.request.*;
import cn.kaziki.nft.turbo.api.collection.response.*;
import cn.yueyu.nft.turbo.api.collection.request.*;
import cn.yueyu.nft.turbo.api.collection.response.*;
import cn.kaziki.nft.turbo.api.collection.service.CollectionManageFacadeService;
import cn.kaziki.nft.turbo.api.goods.constant.GoodsType;
import cn.kaziki.nft.turbo.api.inventory.request.InventoryRequest;
import cn.kaziki.nft.turbo.api.inventory.service.InventoryFacadeService;
import cn.kaziki.nft.turbo.api.user.request.UserQueryRequest;
import cn.kaziki.nft.turbo.api.user.response.UserQueryResponse;
import cn.kaziki.nft.turbo.api.user.response.data.UserInfo;
import cn.kaziki.nft.turbo.api.user.service.UserFacadeService;
import cn.kaziki.nft.turbo.base.response.SingleResponse;
import cn.kaziki.nft.turbo.base.utils.RemoteCallWrapper;
import cn.kaziki.nft.turbo.collection.domain.entity.Collection;
import cn.kaziki.nft.turbo.collection.domain.entity.HeldCollection;
import cn.kaziki.nft.turbo.collection.domain.request.HeldCollectionCreateRequest;
import cn.kaziki.nft.turbo.collection.domain.service.CollectionService;
import cn.kaziki.nft.turbo.collection.domain.service.impl.HeldCollectionService;
import cn.kaziki.nft.turbo.collection.exception.CollectionException;
import cn.kaziki.nft.turbo.collection.infrastructure.mapper.CollectionStreamMapper;
import cn.kaziki.nft.turbo.rpc.facade.Facade;
import cn.hutool.core.lang.Assert;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static cn.kaziki.nft.turbo.api.auth.constant.AuthErrorCode.USER_NOT_EXIST;
import static cn.kaziki.nft.turbo.api.collection.constant.CollectionStateEnum.SUCCEED;
import static cn.kaziki.nft.turbo.api.collection.constant.GoodsSaleBizType.PRIMARY_TRADE;
import static cn.kaziki.nft.turbo.api.common.constant.CommonConstant.SEPARATOR;
import static cn.kaziki.nft.turbo.api.order.constant.OrderErrorCode.*;
import static cn.kaziki.nft.turbo.base.response.ResponseCode.DUPLICATED;
import static cn.kaziki.nft.turbo.collection.exception.CollectionErrorCode.COLLECTION_INVENTORY_UPDATE_FAILED;
import static cn.kaziki.nft.turbo.collection.exception.CollectionErrorCode.COLLECTION_SAVE_FAILED;

/**
 * 藏品rpc服务 实现类
 */
@Slf4j
@DubboService(version = "1.0.0")
public class CollectionManageFacadeServiceImpl implements CollectionManageFacadeService {

    @Autowired
    private CollectionService collectionService;
    @Autowired
    private HeldCollectionService heldCollectionService;
    @Autowired
    private CollectionStreamMapper collectionStreamMapper;
    @Autowired
    private UserFacadeService userFacadeService;
    @Autowired
    private ChainFacadeService chainFacadeService;
    @Autowired
    private InventoryFacadeService inventoryFacadeService;

    // 创建藏品
    @Override
    @Facade
    public CollectionCreateResponse create(CollectionCreateRequest request) {
        Collection collection = collectionService.create(request);

        CollectionCreateResponse response = new CollectionCreateResponse();
        response.setSuccess(true);
        response.setCollectionId(collection.getId());
        return response;
    }

    // 审核通过（触发上链，状态由 MQ 监听器 ChainOperateResultListener 异步更新到 SUCCEED）
    @Override
    @Facade
    public CollectionModifyResponse approveCollection(CollectionStateChangeRequest request) {
        CollectionModifyResponse response = new CollectionModifyResponse();
        // 1.查询藏品
        Collection collection = collectionService.queryById(request.getCollectionId());
        if (collection == null) {
            response.setSuccess(false);
            response.setResponseCode("COLLECTION_NOT_EXIST");
            response.setResponseMessage("藏品不存在");
            return response;
        }

        if(collection.getState() == SUCCEED){
            response.setSuccess(true);
            return response;
        }

        // 2.上链藏品（异步：chain 返回 PROCESSING，MQ 回调 ChainOperateResultListener 更新 state→SUCCEED）
        ChainProcessRequest chainProcessRequest = new ChainProcessRequest();
        chainProcessRequest.setIdentifier(request.getIdentifier() + SEPARATOR + PRIMARY_TRADE);
        chainProcessRequest.setClassId(PRIMARY_TRADE + SEPARATOR + collection.getId());
        chainProcessRequest.setClassName(collection.getName());
        chainProcessRequest.setBizType(ChainOperateBizTypeEnum.COLLECTION.name());
        chainProcessRequest.setBizId(collection.getId().toString());
        var chainRes = chainFacadeService.chain(chainProcessRequest);

        if (!chainRes.getSuccess()) {
            response.setSuccess(false);
            response.setResponseCode(chainRes.getResponseCode());
            response.setResponseMessage(chainRes.getResponseMessage());
            return response;
        }

        response.setSuccess(true);
        response.setCollectionId(request.getCollectionId());
        return response;
    }

    // 下架藏品
    @Override
    public CollectionRemoveResponse remove(CollectionRemoveRequest request) {
        // 1.移除藏品
        CollectionRemoveResponse response = new CollectionRemoveResponse();
        Boolean result = collectionService.remove(request);

        // 2.移除库存缓存
        if (result) {
            InventoryRequest inventoryRequest = new InventoryRequest();
            inventoryRequest.setGoodsId(request.getCollectionId().toString());
            inventoryRequest.setGoodsType(GoodsType.COLLECTION);
            inventoryFacadeService.invalid(inventoryRequest);
        }

        response.setSuccess(result);
        response.setCollectionId(request.getCollectionId());
        return response;
    }

    // 修改藏品价格
    @Override
    public CollectionModifyResponse modifyPrice(CollectionModifyPriceRequest request) {
        Boolean result = collectionService.modifyPrice(request);
        CollectionModifyResponse response = new CollectionModifyResponse();
        response.setSuccess(result);
        response.setCollectionId(request.getCollectionId());
        return response;
    }

    // 藏品评定（稀有度/系列/铸造值/获取途径）
    @Override
    @Facade
    public CollectionModifyResponse assessCollection(CollectionAssessRequest request) {
        Boolean result = collectionService.assess(request);
        CollectionModifyResponse response = new CollectionModifyResponse();
        response.setSuccess(result);
        response.setCollectionId(request.getCollectionId());
        return response;
    }

    // 驳回藏品（只更新状态，不写流水/快照）
    @Override
    @Facade
    public CollectionModifyResponse rejectCollection(CollectionStateChangeRequest request) {
        Boolean result = collectionService.rejectCollection(request);
        CollectionModifyResponse response = new CollectionModifyResponse();
        response.setSuccess(result);
        response.setCollectionId(request.getCollectionId());
        return response;
    }

    // 重新提交审核（INIT → PENDING_REVIEW）
    @Override
    @Facade
    public CollectionModifyResponse resubmitCollection(CollectionStateChangeRequest request) {
        Boolean result = collectionService.resubmitCollection(request);
        CollectionModifyResponse response = new CollectionModifyResponse();
        response.setSuccess(result);
        response.setCollectionId(request.getCollectionId());
        return response;
    }

    // 修改藏品库存
    @Override
    public CollectionModifyResponse modifyInventory(CollectionModifyInventoryRequest request) {
        CollectionModifyResponse response = new CollectionModifyResponse();
        response.setCollectionId(request.getCollectionId());
        // 1.修改藏品库存
        CollectionInventoryModifyResponse modifyResponse = collectionService.modifyInventory(request);

        if (!modifyResponse.getSuccess()) {
            response.setSuccess(false);
            response.setResponseCode(COLLECTION_INVENTORY_UPDATE_FAILED.getCode());
            response.setResponseMessage(COLLECTION_INVENTORY_UPDATE_FAILED.getMessage());
            return response;
        }

        if (modifyResponse.getModifyType() == CollectionInventoryModifyType.UNMODIFIED) {
            response.setSuccess(true);
            return response;
        }

        // 2.更新缓存库存
        InventoryRequest inventoryRequest = new InventoryRequest();
        inventoryRequest.setGoodsId(request.getCollectionId().toString());
        inventoryRequest.setGoodsType(GoodsType.COLLECTION);
        inventoryRequest.setIdentifier(request.getIdentifier());
        inventoryRequest.setInventory(modifyResponse.getQuantityModified());
        SingleResponse<Boolean> inventoryResponse;
        if (modifyResponse.getModifyType() == CollectionInventoryModifyType.INCREASE) {
            inventoryResponse = inventoryFacadeService.increase(inventoryRequest);
        } else {
            inventoryResponse = inventoryFacadeService.decrease(inventoryRequest);
        }

        if (!inventoryResponse.getSuccess()) {
            log.error("modify inventory failed : " + JSON.toJSONString(inventoryResponse));
            throw new CollectionException(COLLECTION_INVENTORY_UPDATE_FAILED);
        }

        response.setSuccess(true);
        return response;
    }

    @Override
    @Facade
    public CollectionAirdropResponse airDrop(CollectionAirDropRequest request) {
        //检查用户是否可被空投，这里比较简单，后续如果节点比较多，可以改成责任链
        UserQueryRequest userQueryRequest = new UserQueryRequest(Long.valueOf(request.getRecipientUserId()));
        UserQueryResponse<UserInfo> userQueryResponse = userFacadeService.query(userQueryRequest);
        checkUser(userQueryResponse);
        //检查藏品是否可被空投，这里比较简单，后续如果节点比较多，可以改成责任链
        Collection collection = collectionService.queryById(request.getCollectionId());
        checkCollection(collection,request.getQuantity());

        CollectionAirdropResponse response = collectionService.airDrop(request, collection);

        //执行失败或幂等成功，则直接返回，不用调上链操作了
        if (!response.getSuccess() || response.getResponseCode().equals(DUPLICATED.name())) {
            return response;
        }

        for (HeldCollectionVO heldCollection : response.getHeldCollections()) {
            ChainProcessRequest chainProcessRequest = new ChainProcessRequest();
            chainProcessRequest.setRecipient(userQueryResponse.getData().getBlockChainUrl());
            chainProcessRequest.setClassId(String.valueOf(heldCollection.getCollectionId()));
            chainProcessRequest.setClassName(heldCollection.getName());
            chainProcessRequest.setSerialNo(heldCollection.getSerialNo());
            chainProcessRequest.setBizId(heldCollection.getId().toString());
            chainProcessRequest.setBizType(ChainOperateBizTypeEnum.HELD_COLLECTION.name());
            chainProcessRequest.setIdentifier(UUID.randomUUID().toString());
            //如果失败了，则依靠定时任务补偿
            ChainProcessResponse<ChainOperationData> chainProcessResponse = RemoteCallWrapper.call(req -> chainFacadeService.mint(req), chainProcessRequest, "mint");
        }
        response.setSuccess(response.getSuccess());
        return response;
    }

    private void checkCollection(Collection collection,Long quantity) {
        if (collection == null) {
            throw new CollectionException(USER_NOT_EXIST);
        }

        if (collection.getState() != SUCCEED) {
            throw new CollectionException(GOODS_NOT_AVAILABLE);
        }

        if (collection.getSaleableInventory() < quantity) {
            throw new CollectionException(INVENTORY_NOT_ENOUGH);
        }
    }

    private static void checkUser(UserQueryResponse<UserInfo> userQueryResponse) {
        if (!userQueryResponse.getSuccess() || userQueryResponse.getData() == null) {
            throw new CollectionException(USER_NOT_EXIST);
        }

        UserInfo userInfo = userQueryResponse.getData();
        if (!userInfo.userCanBuy()) {
            throw new CollectionException(BUYER_STATUS_ABNORMAL);
        }
    }

    // 创建持有藏品（供合成/开盒等跨模块调用）
    @Override
    public SingleResponse<HeldCollectionVO> createHeldCollection(HeldCollectionCreateDTO dto) {
        HeldCollectionCreateRequest req = new HeldCollectionCreateRequest();
        req.setIdentifier(dto.getIdentifier() != null ? dto.getIdentifier() : dto.getBizNo());
        req.setName(dto.getName());
        req.setCover(dto.getCover());
        req.setPurchasePrice(dto.getPurchasePrice());
        req.setGoodsId(dto.getGoodsId());
        req.setRarity(dto.getRarity());
        req.setForgeValue(dto.getForgeValue());
        req.setGoodsType(dto.getGoodsType());
        req.setUserId(dto.getUserId());
        req.setSerialNoBaseId(dto.getSerialNoBaseId());
        req.setBizNo(dto.getBizNo());
        req.setBizType(dto.getBizType());

        HeldCollection result = heldCollectionService.create(req);
        if (result == null) {
            return SingleResponse.fail("HELD_COLLECTION_CREATE_FAILED", "创建持有藏品失败");
        }
        HeldCollectionVO vo = new HeldCollectionVO();
        vo.setId(result.getId().toString());
        vo.setSerialNo(result.getSerialNo());
        vo.setName(result.getName());
        vo.setCover(result.getCover());
        vo.setCollectionId(result.getCollectionId());
        return SingleResponse.of(vo);
    }

    // 批量更新持有藏品状态（lock/unlock/destroy，校验userId归属）
    @Override
    public SingleResponse<Boolean> batchUpdateHeldCollectionState(List<Long> ids, String newState, String userId) {
        List<HeldCollection> list = heldCollectionService.listByIds(ids);
        for (HeldCollection hc : list) {
            if (!userId.equals(hc.getUserId())) {
                return SingleResponse.fail("HELD_COLLECTION_NOT_OWNED", "持有藏品不属于当前用户");
            }
            hc.setState(HeldCollectionState.valueOf(newState));
            Assert.isTrue(heldCollectionService.updateById(hc), () -> new CollectionException(COLLECTION_SAVE_FAILED));
        }
        return SingleResponse.of(true);
    }


}
