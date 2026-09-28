package cn.kaziki.nft.turbo.box.facade;

import cn.kaziki.nft.turbo.api.box.constant.BlindBoxStateEnum;
import cn.kaziki.nft.turbo.api.box.request.BlindBoxCreateRequest;
import cn.kaziki.nft.turbo.api.box.request.BlindBoxModifyInventoryRequest;
import cn.kaziki.nft.turbo.api.box.request.BlindBoxModifyRequest;
import cn.kaziki.nft.turbo.api.box.response.BlindBoxCreateResponse;
import cn.kaziki.nft.turbo.api.box.service.BlindBoxManageFacadeService;
import cn.kaziki.nft.turbo.api.chain.constant.ChainOperateBizTypeEnum;
import cn.kaziki.nft.turbo.api.chain.request.ChainProcessRequest;
import cn.kaziki.nft.turbo.api.chain.service.ChainFacadeService;
import cn.kaziki.nft.turbo.api.collection.constant.GoodsSaleBizType;
import cn.kaziki.nft.turbo.api.goods.constant.GoodsType;
import cn.kaziki.nft.turbo.api.inventory.request.InventoryRequest;
import cn.kaziki.nft.turbo.api.inventory.service.InventoryFacadeService;
import cn.kaziki.nft.turbo.base.response.BaseResponse;
import cn.kaziki.nft.turbo.box.domain.entity.BlindBox;
import cn.kaziki.nft.turbo.box.domain.service.BlindBoxService;
import cn.kaziki.nft.turbo.rpc.facade.Facade;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

import static cn.kaziki.nft.turbo.api.common.constant.CommonConstant.SEPARATOR;

/**
 * 盲盒管理服务
 */
@Slf4j
@DubboService(version = "1.0.0")
public class BlindBoxManageFacadeServiceImpl implements BlindBoxManageFacadeService {

    @Autowired
    private BlindBoxService blindBoxService;

    @Autowired
    private ChainFacadeService chainFacadeService;

    @Autowired
    private InventoryFacadeService inventoryFacadeService;

    // 创建盲盒（INIT，不上链，审核通过后才上链）
    @Override
    @Facade
    public BlindBoxCreateResponse create(BlindBoxCreateRequest request) {
        BlindBoxCreateResponse response = new BlindBoxCreateResponse();
        BlindBox blindBox = blindBoxService.create(request);
        response.setSuccess(true);
        response.setBlindBoxId(blindBox.getId());
        return response;
    }

    /**
     * 审核通过
     * @param boxId
     * @return
     */
    @Override
    @Facade
    public BaseResponse approve(Long boxId) {
        BlindBox blindBox = blindBoxService.getById(boxId);
        if (blindBox == null) {
            return err("BLIND_BOX_NOT_EXIST", "盲盒不存在");
        }
        if (!BlindBoxStateEnum.PENDING_REVIEW.equals(blindBox.getState())) {
            return err("STATE_ERROR", "仅待审核状态可通过");
        }
        // 先上链，成功后才改状态
        ChainProcessRequest chainRequest = new ChainProcessRequest();
        chainRequest.setIdentifier("APPROVE_" + boxId + "_" + System.currentTimeMillis());
        chainRequest.setClassId(GoodsSaleBizType.BLIND_BOX_TRADE + SEPARATOR + blindBox.getId());
        chainRequest.setClassName(blindBox.getName());
        chainRequest.setBizType(ChainOperateBizTypeEnum.BLIND_BOX.name());
        chainRequest.setBizId(blindBox.getId().toString());
        var chainRes = chainFacadeService.chain(chainRequest);
        if (!chainRes.getSuccess()) {
            return err(chainRes.getResponseCode(), chainRes.getResponseMessage());
        }
        blindBox.setState(BlindBoxStateEnum.SUCCEED);
        blindBoxService.updateById(blindBox);
        return ok();
    }

    @Override
    @Facade
    public BaseResponse reject(Long boxId) {
        BlindBox blindBox = blindBoxService.getById(boxId);
        if (blindBox == null) {
            return err("BLIND_BOX_NOT_EXIST", "盲盒不存在");
        }
        if (!BlindBoxStateEnum.PENDING_REVIEW.equals(blindBox.getState())) {
            return err("STATE_ERROR", "仅待审核状态可驳回");
        }
        blindBox.setState(BlindBoxStateEnum.INIT);
        blindBoxService.updateById(blindBox);
        return ok();
    }

    @Override
    @Facade
    public BaseResponse modifyInventory(BlindBoxModifyInventoryRequest request) {
        // 1. DB 更新（含幂等校验 + 流水 + 条目展开）
        Long boxId = blindBoxService.modifyInventory(request);

        // 2. Redis 同步库存增量
        InventoryRequest invReq = new InventoryRequest();
        invReq.setGoodsId(boxId.toString());
        invReq.setGoodsType(GoodsType.BLIND_BOX);
        invReq.setIdentifier(request.getIdentifier());
        long additionalQuantity = request.getItems().stream()
                .mapToLong(item -> item.getQuantity()).sum();
        invReq.setInventory(additionalQuantity);
        inventoryFacadeService.increase(invReq);

        return ok();
    }

    @Override
    @Facade
    public BaseResponse modify(BlindBoxModifyRequest request) {
        BlindBox blindBox = blindBoxService.getById(request.getBoxId());
        if (blindBox == null) {
            return err("BLIND_BOX_NOT_EXIST", "盲盒不存在");
        }
        // 逐字段覆盖（null 保留原值）
        if (request.getName() != null) blindBox.setName(request.getName());
        if (request.getCover() != null) blindBox.setCover(request.getCover());
        if (request.getDetail() != null) blindBox.setDetail(request.getDetail());
        if (request.getPrice() != null) blindBox.setPrice(request.getPrice());
        if (request.getQuantity() != null) blindBox.setQuantity(request.getQuantity());
        if (request.getSaleTime() != null) blindBox.setSaleTime(request.getSaleTime());
        if (request.getSeriesId() != null) blindBox.setSeriesId(request.getSeriesId());
        blindBoxService.updateById(blindBox);
        return ok();
    }

    private BaseResponse ok() {
        BaseResponse r = new BaseResponse();
        r.setSuccess(true);
        return r;
    }

    private BaseResponse err(String code, String msg) {
        BaseResponse r = new BaseResponse();
        r.setSuccess(false);
        r.setResponseCode(code);
        r.setResponseMessage(msg);
        return r;
    }

}
