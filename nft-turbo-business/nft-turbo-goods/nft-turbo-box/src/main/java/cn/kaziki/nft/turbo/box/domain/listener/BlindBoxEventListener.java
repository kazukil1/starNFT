package cn.kaziki.nft.turbo.box.domain.listener;

import cn.kaziki.nft.turbo.api.chain.constant.ChainOperateBizTypeEnum;
import cn.kaziki.nft.turbo.api.chain.request.ChainProcessRequest;
import cn.kaziki.nft.turbo.api.chain.service.ChainFacadeService;
import cn.kaziki.nft.turbo.api.collection.constant.GoodsSaleBizType;
import cn.kaziki.nft.turbo.api.goods.constant.GoodsType;
import cn.kaziki.nft.turbo.api.star.constant.StarChangeType;
import cn.kaziki.nft.turbo.api.star.request.StarChangeRequest;
import cn.kaziki.nft.turbo.api.star.service.StarAccountFacadeService;
import cn.kaziki.nft.turbo.api.user.request.UserQueryRequest;
import cn.kaziki.nft.turbo.api.user.response.UserQueryResponse;
import cn.kaziki.nft.turbo.api.user.response.data.UserInfo;
import cn.kaziki.nft.turbo.api.user.service.UserFacadeService;
import cn.kaziki.nft.turbo.base.utils.RemoteCallWrapper;
import cn.kaziki.nft.turbo.box.domain.entity.BlindBoxItem;
import cn.kaziki.nft.turbo.box.domain.listener.event.BlindBoxOpenEvent;
import cn.kaziki.nft.turbo.box.domain.service.BlindBoxItemService;
import cn.kaziki.nft.turbo.box.exception.BlindBoxException;
import cn.kaziki.nft.turbo.collection.domain.entity.HeldCollection;
import cn.kaziki.nft.turbo.collection.domain.request.HeldCollectionCreateRequest;
import cn.kaziki.nft.turbo.collection.domain.service.impl.HeldCollectionService;
import cn.hutool.core.lang.Assert;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import static cn.kaziki.nft.turbo.api.common.constant.CommonConstant.SEPARATOR;
import static cn.kaziki.nft.turbo.box.exception.BlindBoxErrorCode.BLIND_BOX_ITEM_SAVE_FAILED;
import static cn.kaziki.nft.turbo.box.exception.BlindBoxErrorCode.BLIND_BOX_OPEN_FAILED;


@Component
public class BlindBoxEventListener {
    @Autowired
    private UserFacadeService userFacadeService;
    @Autowired
    private ChainFacadeService chainFacadeService;
    @Autowired
    private BlindBoxItemService blindBoxItemService;
    @Autowired
    private HeldCollectionService heldCollectionService;
    @Autowired
    private StarAccountFacadeService starAccountFacadeService;

    @EventListener(value = BlindBoxOpenEvent.class)
    @Async("blindBoxListenExecutor")
    public void onApplicationEvent(BlindBoxOpenEvent event) {
        Long blindBoxItemId = (Long) event.getSource();

        // 1.查询出更新后的最新值，避免后续 cas 操作失败
        BlindBoxItem blindBoxItem = blindBoxItemService.getById(blindBoxItemId);

        // 2.根据 starAmount 走不同分支
        if (blindBoxItem.getStarAmount() != null && blindBoxItem.getStarAmount() > 0) {
            // STAR 分支：发星尘，不上链
            handleStarOpen(blindBoxItem);
        } else {
            // COLLECTION 分支：创建 HeldCollection + 上链
            handleCollectionOpen(blindBoxItem);
        }
    }

    /** STAR 开盒：发星尘 */
    private void handleStarOpen(BlindBoxItem blindBoxItem) {
        StarChangeRequest request = new StarChangeRequest();
        request.setUserId(blindBoxItem.getUserId());
        request.setAmount(blindBoxItem.getStarAmount());
        request.setChangeType(StarChangeType.BOX_DROP);
        request.setBizNo(blindBoxItem.getId().toString());
        request.setIdentifier(blindBoxItem.getId() + "_STAR");
        starAccountFacadeService.increase(request);

        blindBoxItem.openSuccess();
        var saveResult = blindBoxItemService.updateById(blindBoxItem);
        Assert.isTrue(saveResult, () -> new BlindBoxException(BLIND_BOX_ITEM_SAVE_FAILED));
    }

    /** COLLECTION 开盒：创建 HeldCollection + 上链 */
    private void handleCollectionOpen(BlindBoxItem blindBoxItem) {
        HeldCollectionCreateRequest heldCollectionCreateRequest = getHeldCollectionCreateRequest(blindBoxItem);
        //创建藏品
        var heldCollection = heldCollectionService.create(heldCollectionCreateRequest);
        Assert.notNull(heldCollection, () -> new BlindBoxException(BLIND_BOX_OPEN_FAILED));

        // 上链（异步，失败由 ChainProcessJob 定时任务补偿）
        ChainProcessRequest chainProcessRequest = getChainProcessRequest(blindBoxItem, heldCollection);
        RemoteCallWrapper.call(req -> chainFacadeService.mint(req), chainProcessRequest, "mint");

        // 对齐空投模式：mint RPC 成功即链模块已受理，条目直接标记 SUCCEED
        blindBoxItem.openSuccess();
        var saveResult = blindBoxItemService.updateById(blindBoxItem);
        Assert.isTrue(saveResult, () -> new BlindBoxException(BLIND_BOX_ITEM_SAVE_FAILED));
    }

    private @NotNull ChainProcessRequest getChainProcessRequest(BlindBoxItem blindBoxItem, HeldCollection heldCollection) {
        UserQueryRequest userQueryRequest = new UserQueryRequest(Long.valueOf(blindBoxItem.getUserId()));
        UserQueryResponse<UserInfo> userQueryResponse = userFacadeService.query(userQueryRequest);
        ChainProcessRequest chainProcessRequest = new ChainProcessRequest();
        chainProcessRequest.setRecipient(userQueryResponse.getData().getBlockChainUrl());
        chainProcessRequest.setClassId(GoodsSaleBizType.BLIND_BOX_TRADE + SEPARATOR + blindBoxItem.getBlindBoxId());
        chainProcessRequest.setClassName(blindBoxItem.getName());
        chainProcessRequest.setSerialNo(heldCollection.getSerialNo());
        chainProcessRequest.setBizId(heldCollection.getId().toString());
        chainProcessRequest.setBizType(ChainOperateBizTypeEnum.HELD_COLLECTION.name());
        chainProcessRequest.setIdentifier(blindBoxItem.getId().toString());
        return chainProcessRequest;
    }

    private static @NotNull HeldCollectionCreateRequest getHeldCollectionCreateRequest(BlindBoxItem blindBoxItem) {
        HeldCollectionCreateRequest heldCollectionCreateRequest = new HeldCollectionCreateRequest();
        heldCollectionCreateRequest.setName(blindBoxItem.getCollectionName());
        heldCollectionCreateRequest.setCover(blindBoxItem.getCollectionCover());
        heldCollectionCreateRequest.setBizNo(blindBoxItem.getOrderId());
        heldCollectionCreateRequest.setBizType(GoodsSaleBizType.BLIND_BOX_TRADE.name());
        heldCollectionCreateRequest.setPurchasePrice(blindBoxItem.getPurchasePrice());
        heldCollectionCreateRequest.setReferencePrice(blindBoxItem.getReferencePrice());
        heldCollectionCreateRequest.setRarity(blindBoxItem.getRarity());
        // 盲盒条目关联藏品主档，铸造值按稀有度基准值带出
        heldCollectionCreateRequest.setForgeValue(blindBoxItem.getRarity() == null ? null : blindBoxItem.getRarity().getBaseForgeValue());
        heldCollectionCreateRequest.setUserId(blindBoxItem.getUserId());
        heldCollectionCreateRequest.setGoodsId(blindBoxItem.getCollectionId() != null ? blindBoxItem.getCollectionId() : blindBoxItem.getId());
        heldCollectionCreateRequest.setGoodsType(GoodsType.BLIND_BOX.name());
        heldCollectionCreateRequest.setSerialNoBaseId(blindBoxItem.getBlindBoxId().toString());
        return heldCollectionCreateRequest;
    }
}
