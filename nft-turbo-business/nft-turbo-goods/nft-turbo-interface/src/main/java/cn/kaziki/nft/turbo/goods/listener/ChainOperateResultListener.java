package cn.kaziki.nft.turbo.goods.listener;

import cn.kaziki.nft.turbo.api.box.constant.BlindBoxStateEnum;
import cn.kaziki.nft.turbo.api.chain.model.ChainOperateBody;
import cn.kaziki.nft.turbo.api.chain.response.data.ChainResultData;
import cn.kaziki.nft.turbo.api.collection.constant.CollectionStateEnum;
import cn.kaziki.nft.turbo.api.collection.constant.HeldCollectionState;
import cn.kaziki.nft.turbo.api.goods.constant.GoodsType;
import cn.kaziki.nft.turbo.api.inventory.request.InventoryRequest;
import cn.kaziki.nft.turbo.api.inventory.service.InventoryFacadeService;
import cn.kaziki.nft.turbo.base.response.SingleResponse;
import cn.kaziki.nft.turbo.box.domain.entity.BlindBox;
import cn.kaziki.nft.turbo.box.domain.service.BlindBoxService;
import cn.kaziki.nft.turbo.box.exception.BlindBoxException;
import cn.kaziki.nft.turbo.collection.domain.entity.Collection;
import cn.kaziki.nft.turbo.collection.domain.entity.HeldCollection;
import cn.kaziki.nft.turbo.collection.domain.request.HeldCollectionActiveRequest;
import cn.kaziki.nft.turbo.collection.domain.service.CollectionService;
import cn.kaziki.nft.turbo.collection.domain.service.impl.HeldCollectionService;
import cn.kaziki.nft.turbo.collection.exception.CollectionException;
import cn.kaziki.turbo.stream.consumer.AbstractStreamConsumer;
import cn.kaziki.turbo.stream.param.MessageBody;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.Date;
import java.util.function.Consumer;

import static cn.kaziki.nft.turbo.box.exception.BlindBoxErrorCode.BLIND_BOX_INVENTORY_UPDATE_FAILED;
import static cn.kaziki.nft.turbo.box.exception.BlindBoxErrorCode.BLIND_BOX_NOT_EXIST;
import static cn.kaziki.nft.turbo.collection.exception.CollectionErrorCode.COLLECTION_QUERY_FAIL;
import static cn.kaziki.nft.turbo.collection.exception.CollectionErrorCode.HELD_COLLECTION_QUERY_FAIL;

/**
 * 链操作结果监听器
 */
@Slf4j
@Component
public class ChainOperateResultListener extends AbstractStreamConsumer {

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private HeldCollectionService heldCollectionService;

    @Autowired
    private InventoryFacadeService inventoryFacadeService;

    @Autowired
    private BlindBoxService blindBoxService;

    @Bean
    Consumer<Message<MessageBody>> chain() {
        return msg -> {
            ChainOperateBody chainOperateBody = getMessage(msg, ChainOperateBody.class);
            ChainResultData chainResultData = chainOperateBody.getChainResultData();
            boolean result;
            switch (chainOperateBody.getOperateType()) {
                case COLLECTION_CHAIN:
                    // 1.获取当前藏品
                    Collection collection = collectionService.getById(chainOperateBody.getBizId());
                    if (null == collection) {
                        throw new CollectionException(COLLECTION_QUERY_FAIL);
                    }
                    // 2.将新建藏品加入缓存
                    initInventory(collection.getId().toString(), GoodsType.COLLECTION, collection.getQuantity(), collection.getId().toString());

                    // 3.更新状态
                    collection.setState(CollectionStateEnum.SUCCEED);
                    collection.setSyncChainTime(new Date());
                    result = collectionService.updateById(collection);
                    Assert.isTrue(result, "collection chain failed");
                    break;
                case BLIND_BOX_CHAIN:
                    BlindBox blindBox = blindBoxService.queryById(Long.valueOf(chainOperateBody.getBizId()));
                    if (null == blindBox) {
                        throw new BlindBoxException(BLIND_BOX_NOT_EXIST);
                    }
                    //先写缓存，写成功再更新状态
                    initInventory(blindBox.getId().toString(), GoodsType.BLIND_BOX, blindBox.getQuantity(), blindBox.getId().toString());

                    //更新状态
                    blindBox.setState(BlindBoxStateEnum.SUCCEED);
                    blindBox.setSyncChainTime(new Date());
                    result = blindBoxService.updateById(blindBox);
                    Assert.isTrue(result, "blind box chain failed");
                    break;
                case COLLECTION_MINT:
                    HeldCollectionActiveRequest request = new HeldCollectionActiveRequest();
                    request.setHeldCollectionId(chainOperateBody.getBizId());
                    request.setIdentifier(chainOperateBody.getOperateInfoId().toString());
                    request.setNftId(chainResultData.getNftId());
                    request.setTxHash(chainResultData.getTxHash());
                    result = heldCollectionService.active(request);
                    Assert.isTrue(result, "active held collection failed");
                    break;
                case COLLECTION_TRANSFER:
                    //藏品铸造成功有nftId和txHash
                    HeldCollection transferCollection = heldCollectionService.queryById(Long.valueOf(chainOperateBody.getBizId()));
                    if (null == transferCollection || !transferCollection.getState().equals(HeldCollectionState.INIT)) {
                        throw new CollectionException(HELD_COLLECTION_QUERY_FAIL);
                    }
                    transferCollection.actived(chainResultData.getNftId(), chainResultData.getTxHash());
                    result = heldCollectionService.updateById(transferCollection);
                    Assert.isTrue(result, "collection transfer failed");
                    break;
                case COLLECTION_DESTROY:
                    //藏品铸造成功有nftId和txHash
                    HeldCollection destroyCollection = heldCollectionService.queryById(Long.valueOf(chainOperateBody.getBizId()));

                    if (null == destroyCollection) {
                        throw new CollectionException(HELD_COLLECTION_QUERY_FAIL);
                    }

                    if (destroyCollection.getState().equals(HeldCollectionState.DESTROYED)) {
                        return;
                    }

                    if (!destroyCollection.getState().equals(HeldCollectionState.DESTROYING)) {
                        throw new CollectionException(HELD_COLLECTION_QUERY_FAIL);
                    }

                    destroyCollection.destroyed();
                    result = heldCollectionService.updateById(destroyCollection);
                    Assert.isTrue(result, "collection destroy failed");
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + chainOperateBody.getBizType().name());

            }
        };
    }

    private void initInventory(String goodsId, GoodsType goodsType, Long inventory, String identifier) {
        InventoryRequest inventoryRequest = new InventoryRequest();
        inventoryRequest.setGoodsId(goodsId);
        inventoryRequest.setGoodsType(goodsType);
        inventoryRequest.setInventory(inventory);
        // 将商品id作为幂等
        inventoryRequest.setIdentifier(identifier);
        SingleResponse<Boolean> inventoryResponse = inventoryFacadeService.init(inventoryRequest);
        if (!inventoryResponse.getSuccess()) {
            throw new BlindBoxException(BLIND_BOX_INVENTORY_UPDATE_FAILED);
        }
    }

}
