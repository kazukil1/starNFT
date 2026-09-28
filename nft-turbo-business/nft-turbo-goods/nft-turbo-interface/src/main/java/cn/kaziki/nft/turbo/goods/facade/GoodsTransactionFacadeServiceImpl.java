package cn.kaziki.nft.turbo.goods.facade;

import cn.kaziki.nft.turbo.api.goods.constant.GoodsType;
import cn.kaziki.nft.turbo.api.goods.request.*;
import cn.yueyu.nft.turbo.api.goods.request.*;
import cn.kaziki.nft.turbo.api.goods.response.GoodsSaleResponse;
import cn.kaziki.nft.turbo.api.goods.service.GoodsTransactionFacadeService;
import cn.kaziki.nft.turbo.box.domain.service.BlindBoxService;
import cn.kaziki.nft.turbo.collection.domain.service.CollectionService;
import cn.kaziki.nft.turbo.lock.DistributeLock;
import cn.kaziki.nft.turbo.rpc.facade.Facade;
import cn.kaziki.nft.turbo.tcc.entity.TransCancelSuccessType;
import cn.kaziki.nft.turbo.tcc.entity.TransConfirmSuccessType;
import cn.kaziki.nft.turbo.tcc.entity.TransTrySuccessType;
import cn.kaziki.nft.turbo.tcc.request.TccRequest;
import cn.kaziki.nft.turbo.tcc.response.TransactionCancelResponse;
import cn.kaziki.nft.turbo.tcc.response.TransactionConfirmResponse;
import cn.kaziki.nft.turbo.tcc.response.TransactionTryResponse;
import cn.kaziki.nft.turbo.tcc.service.TransactionLogService;
import cn.hutool.core.lang.Assert;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Hollis
 */
@DubboService(version = "1.0.0")
public class GoodsTransactionFacadeServiceImpl implements GoodsTransactionFacadeService {

    private static final String ERROR_CODE_UNSUPPORTED_GOODS_TYPE = "UNSUPPORTED_GOODS_TYPE";

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private BlindBoxService blindBoxService;

    @Autowired
    private TransactionLogService transactionLogService;

    // 锁定库存
    @Override
    @Facade
    @Transactional(rollbackFor = Exception.class)
    @DistributeLock(keyExpression = "#request.bizNo",scene = "NORMAL_BUY_GOODS")
    public GoodsSaleResponse tryDecreaseInventory(GoodsSaleRequest request) {
        GoodsFreezeInventoryRequest goodsTrySaleRequest = new GoodsFreezeInventoryRequest(request.getBizNo(), request.getGoodsId(), request.getQuantity());
        GoodsType goodsType = GoodsType.valueOf(request.getGoodsType());
        // 1.记录事务日志（TRY）
        TransactionTryResponse transactionTryResponse = transactionLogService.tryTransaction(new TccRequest(request.getBizNo(), "normalBuy", goodsType.name()));
        Assert.isTrue(transactionTryResponse.getSuccess(), "transaction try failed");

        // 2.冻结库存
        if (transactionTryResponse.getTransTrySuccessType() == TransTrySuccessType.TRY_SUCCESS) {
            Boolean freezeResult = switch (goodsType) {
                case BLIND_BOX -> blindBoxService.freezeInventory(goodsTrySaleRequest);
                case COLLECTION -> collectionService.freezeInventory(goodsTrySaleRequest);
                default -> throw new UnsupportedOperationException(ERROR_CODE_UNSUPPORTED_GOODS_TYPE);
            };
            Assert.isTrue(freezeResult, "freeze inventory failed");
            GoodsSaleResponse response = new GoodsSaleResponse();
            response.setSuccess(true);
            return response;
        }

        return new GoodsSaleResponse.GoodsResponseBuilder().buildSuccess();
    }

    // 解冻并扣减库存
    @Override
    @Facade
    @Transactional(rollbackFor = Exception.class)
    @DistributeLock(keyExpression = "#request.bizNo",scene = "NORMAL_BUY_GOODS")
    public GoodsSaleResponse confirmDecreaseInventory(GoodsSaleRequest request) {
        GoodsUnfreezeAndSaleRequest unfreezeAndSaleRequest = new GoodsUnfreezeAndSaleRequest(request.getBizNo(), request.getGoodsId(), request.getQuantity());
        GoodsType goodsType = GoodsType.valueOf(request.getGoodsType());
        // 1.更新事务日志（CONFIEM）
        TransactionConfirmResponse transactionConfirmResponse = transactionLogService.confirmTransaction(new TccRequest(request.getBizNo(), "normalBuy", goodsType.name()));
        Assert.isTrue(transactionConfirmResponse.getSuccess(), "transaction confirm failed");

        // 2.成功并且不是幂等，解冻并扣减库存
        if (transactionConfirmResponse.getTransConfirmSuccessType() == TransConfirmSuccessType.CONFIRM_SUCCESS) {
            Boolean unfreezeResult = switch (goodsType) {
                case BLIND_BOX -> blindBoxService.unfreezeAndSale(unfreezeAndSaleRequest);
                case COLLECTION -> collectionService.unfreezeAndSale(unfreezeAndSaleRequest);
                default -> throw new UnsupportedOperationException(ERROR_CODE_UNSUPPORTED_GOODS_TYPE);
            };
            Assert.isTrue(unfreezeResult, "unfreeze inventory failed");

            GoodsSaleResponse response = new GoodsSaleResponse();
            response.setSuccess(true);
            return response;
        }

        return new GoodsSaleResponse.GoodsResponseBuilder().buildSuccess();
    }

    // 解锁库存
    @Override
    @Facade
    @Transactional(rollbackFor = Exception.class)
    @DistributeLock(keyExpression = "#request.bizNo",scene = "NORMAL_BUY_GOODS")
    public GoodsSaleResponse cancelDecreaseInventory(GoodsSaleRequest request) {
        GoodsType goodsType = GoodsType.valueOf(request.getGoodsType());
        // 1.更新或新增（空回滚）事务日志（CANCEL）
        TransactionCancelResponse transactionCancelResponse = transactionLogService.cancelTransaction(new TccRequest(request.getBizNo(), "normalBuy", goodsType.name()));
        Assert.isTrue(transactionCancelResponse.getSuccess(), "transaction cancel failed");

        // 2.
        // 情况一：Try成功后的Cancel，解冻库存
        if (transactionCancelResponse.getTransCancelSuccessType() == TransCancelSuccessType.CANCEL_AFTER_TRY_SUCCESS) {
            GoodsUnfreezeInventoryRequest unfreezeInventoryRequest = new GoodsUnfreezeInventoryRequest(request.getBizNo(), request.getGoodsId(), request.getQuantity());
            Boolean unfreezeResult = switch (goodsType) {
                case BLIND_BOX -> blindBoxService.unfreezeInventory(unfreezeInventoryRequest);
                case COLLECTION -> collectionService.unfreezeInventory(unfreezeInventoryRequest);
                default -> throw new UnsupportedOperationException(ERROR_CODE_UNSUPPORTED_GOODS_TYPE);
            };
            Assert.isTrue(unfreezeResult, "unfreeze inventory failed");
        }
        // 情况二：Confirm成功后的Cancel，回滚可售库存
        if (transactionCancelResponse.getTransCancelSuccessType() == TransCancelSuccessType.CANCEL_AFTER_CONFIRM_SUCCESS) {
            GoodsCancelSaleRequest goodsCancelSaleRequest = new GoodsCancelSaleRequest(request.getBizNo(), request.getGoodsId(), request.getQuantity(),"tcc回滚库存");
            Boolean cancelResult = switch (goodsType) {
                case BLIND_BOX -> blindBoxService.cancel(goodsCancelSaleRequest);
                case COLLECTION -> collectionService.cancel(goodsCancelSaleRequest);
                default -> throw new UnsupportedOperationException(ERROR_CODE_UNSUPPORTED_GOODS_TYPE);
            };
            Assert.isTrue(cancelResult, "cancel inventory failed");
        }

        // 情况三：如果发生空回滚，或者回滚幂等，则不进行解冻库存操作
        GoodsSaleResponse response = new GoodsSaleResponse();
        response.setSuccess(true);
        return response;
    }
}
