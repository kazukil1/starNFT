package cn.kaziki.nft.turbo.goods.facade;

import cn.kaziki.nft.turbo.api.box.model.BlindBoxVO;
import cn.kaziki.nft.turbo.api.box.service.BlindBoxReadFacadeService;
import cn.kaziki.nft.turbo.api.collection.model.CollectionVO;
import cn.kaziki.nft.turbo.api.collection.service.CollectionReadFacadeService;
import cn.kaziki.nft.turbo.api.goods.constant.GoodsEvent;
import cn.kaziki.nft.turbo.api.goods.constant.GoodsType;
import cn.kaziki.nft.turbo.api.goods.model.BaseGoodsVO;
import cn.kaziki.nft.turbo.api.goods.model.GoodsStreamVO;
import cn.kaziki.nft.turbo.api.goods.request.GoodsBookRequest;
import cn.kaziki.nft.turbo.api.goods.request.GoodsCancelSaleRequest;
import cn.kaziki.nft.turbo.api.goods.request.GoodsSaleRequest;
import cn.kaziki.nft.turbo.api.goods.request.GoodsTrySaleRequest;
import cn.kaziki.nft.turbo.api.goods.response.GoodsBookResponse;
import cn.kaziki.nft.turbo.api.goods.response.GoodsSaleResponse;
import cn.kaziki.nft.turbo.api.goods.service.GoodsFacadeService;
import cn.kaziki.nft.turbo.base.response.SingleResponse;
import cn.kaziki.nft.turbo.box.domain.entity.BlindBoxInventoryStream;
import cn.kaziki.nft.turbo.box.domain.request.BlindBoxAssignRequest;
import cn.kaziki.nft.turbo.box.domain.service.BlindBoxService;
import cn.kaziki.nft.turbo.box.infrastructure.mapper.BlindBoxInventoryStreamMapper;
import cn.kaziki.nft.turbo.collection.domain.entity.Collection;
import cn.kaziki.nft.turbo.collection.domain.entity.CollectionInventoryStream;
import cn.kaziki.nft.turbo.collection.domain.entity.HeldCollection;
import cn.kaziki.nft.turbo.collection.domain.request.HeldCollectionCreateRequest;
import cn.kaziki.nft.turbo.collection.domain.service.CollectionService;
import cn.kaziki.nft.turbo.collection.domain.service.impl.HeldCollectionService;
import cn.kaziki.nft.turbo.collection.infrastructure.mapper.CollectionInventoryStreamMapper;
import cn.kaziki.nft.turbo.api.star.model.StarVO;
import cn.kaziki.nft.turbo.api.star.service.StarReadFacadeService;
import cn.kaziki.nft.turbo.star.domain.entity.StarInventoryStream;
import cn.kaziki.nft.turbo.star.domain.service.StarService;
import cn.kaziki.nft.turbo.star.infrastructure.mapper.StarInventoryStreamMapper;
import cn.kaziki.nft.turbo.goods.entity.convertor.GoodsStreamConvertor;
import cn.kaziki.nft.turbo.goods.service.GoodsBookService;
import cn.kaziki.nft.turbo.goods.service.HotGoodsService;
import cn.kaziki.nft.turbo.rpc.facade.Facade;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphO;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 商品聚合服务
 */
@DubboService(version = "1.0.0")
@Slf4j
public class GoodsFacadeServiceImpl implements GoodsFacadeService {

    private static final String ERROR_CODE_UNSUPPORTED_GOODS_TYPE = "UNSUPPORTED_GOODS_TYPE";

    @Autowired
    private CollectionReadFacadeService collectionFacadeService;
    @Autowired
    private GoodsBookService goodsBookService;
    @Autowired
    private CollectionInventoryStreamMapper collectionInventoryStreamMapper;
    @Autowired
    private CollectionService collectionService;
    @Autowired
    private HeldCollectionService heldCollectionService;
    @Autowired
    private BlindBoxService blindBoxService;
    @Autowired
    private BlindBoxReadFacadeService blindBoxReadFacadeService;
    @Autowired
    private BlindBoxInventoryStreamMapper blindBoxInventoryStreamMapper;
    @Autowired
    private HotGoodsService hotGoodsService;

    @Autowired
    private StarService starService;

    @Autowired
    private StarReadFacadeService starReadFacadeService;

    @Autowired
    private StarInventoryStreamMapper starInventoryStreamMapper;

    // 获取商品
    @Override
    public BaseGoodsVO getGoods(String goodsId, GoodsType goodsType) {
        return switch (goodsType){
            case COLLECTION -> {
                // facade是只读的，所以这里直接调用service获取藏品详情
                SingleResponse<CollectionVO> response = collectionFacadeService.queryById(Long.valueOf(goodsId));
                if(response.getSuccess()){
                    yield response.getData();
                }
                yield null;
            }
            case BLIND_BOX -> {
                SingleResponse<BlindBoxVO> response = blindBoxReadFacadeService.queryById(Long.valueOf(goodsId));
                if (response.getSuccess()) {
                    yield response.getData();
                }
                yield null;
            }
            case STAR -> {
                SingleResponse<StarVO> response = starReadFacadeService.queryById(Long.valueOf(goodsId));
                if (response.getSuccess()) {
                    yield response.getData();
                }
                yield null;
            }
            default -> throw new UnsupportedOperationException("ERROR_CODE_UNSUPPORTED_GOODS_TYPE");
        };
    }

    // 预约商品
    @Override
    @Facade
    public GoodsBookResponse book(GoodsBookRequest request) {
        BaseGoodsVO goodsVO = this.getGoods(request.getGoodsId(), request.getGoodsType());
        if (goodsVO.canBookNow()) {
            return goodsBookService.book(request);
        }
        throw new RuntimeException("GOODS_CAN_NOT_BOOK_NOW");
    }

    // 商品是否已被预约
    @Override
    @Facade
    public Boolean isGoodsBooked(String goodsId, GoodsType goodsType, String buyerId) {
        return goodsBookService.isBooked(goodsId, goodsType, buyerId);
    }

    // 获取商品库存流水
    @Override
    public GoodsStreamVO getGoodsInventoryStream(String goodsId, GoodsType goodsType, GoodsEvent goodsEvent, String identifier) {
        return switch (goodsType) {
            case COLLECTION -> {
                CollectionInventoryStream collectionInventoryStream = collectionInventoryStreamMapper.selectByIdentifier(identifier, goodsEvent.name(), Long.valueOf(goodsId));
                yield GoodsStreamConvertor.INSTANCE.mapToVo(collectionInventoryStream);
            }

            case BLIND_BOX -> {
                BlindBoxInventoryStream blindBoxInventoryStream = blindBoxInventoryStreamMapper.selectByIdentifier(identifier, goodsEvent.name(), Long.valueOf(goodsId));
                yield GoodsStreamConvertor.INSTANCE.mapToVo(blindBoxInventoryStream);
            }
            case STAR -> {
                StarInventoryStream stream = starInventoryStreamMapper.selectByIdentifier(identifier, goodsEvent.name(), Long.valueOf(goodsId));
                yield GoodsStreamConvertor.INSTANCE.mapToVo(stream);
            }
            default -> throw new UnsupportedOperationException(ERROR_CODE_UNSUPPORTED_GOODS_TYPE);
        };
    }

    @Override
    public GoodsSaleResponse sale(GoodsSaleRequest request) {
        GoodsSaleResponse response = new GoodsSaleResponse();
        if (SphO.entry("GOODS_SALE", EntryType.IN, 1, request.getGoodsId() + "_" + request.getGoodsType())) {
            try {
                GoodsTrySaleRequest goodsTrySaleRequest = new GoodsTrySaleRequest(request.getIdentifier(), request.getGoodsId(), request.getQuantity(), request.getExtendInfo());
                GoodsType goodsType = GoodsType.valueOf(request.getGoodsType());

                Boolean trySaleResult = switch (goodsType) {
                    case BLIND_BOX -> blindBoxService.sale(goodsTrySaleRequest);
                    case COLLECTION -> collectionService.sale(goodsTrySaleRequest);
                    case STAR -> starService.sale(goodsTrySaleRequest);
                    default -> throw new UnsupportedOperationException(ERROR_CODE_UNSUPPORTED_GOODS_TYPE);
                };
                response.setSuccess(trySaleResult);
                return response;
            } finally {
                SphO.exit();
            }
        } else {
            log.warn("GOODS_SALE 触发限流...");
            response.setSuccess(false);
            return response;
        }
    }

    @Override
    public GoodsSaleResponse saleWithoutHint(GoodsSaleRequest request) {
        GoodsSaleResponse response = new GoodsSaleResponse();
        if (SphO.entry("GOODS_SALE", EntryType.IN, 1, request.getGoodsId() + "_" + request.getGoodsType())) {
            try {
                GoodsTrySaleRequest goodsTrySaleRequest = new GoodsTrySaleRequest(request.getIdentifier(), request.getGoodsId(), request.getQuantity(), request.getExtendInfo());

                GoodsType goodsType = GoodsType.valueOf(request.getGoodsType());

                Boolean trySaleResult = switch (goodsType) {
                    case BLIND_BOX -> blindBoxService.saleWithoutHint(goodsTrySaleRequest);
                    case COLLECTION -> collectionService.saleWithoutHint(goodsTrySaleRequest);
                    case STAR -> starService.saleWithoutHint(goodsTrySaleRequest);
                    default -> throw new UnsupportedOperationException(ERROR_CODE_UNSUPPORTED_GOODS_TYPE);
                };
                response.setSuccess(trySaleResult);
                return response;
            }finally {
                SphO.exit();
            }
        } else {
            log.warn("GOODS_SALE 触发限流...");
            response.setSuccess(false);
            return response;
        }
    }

    @Override
    public GoodsSaleResponse cancelSale(GoodsSaleRequest request) {
        GoodsCancelSaleRequest goodsCancelSaleRequest = new GoodsCancelSaleRequest(request.getIdentifier(), request.getGoodsId(), request.getQuantity(), request.getExtendInfo());

        GoodsType goodsType = GoodsType.valueOf(request.getGoodsType());

        Boolean result = switch (goodsType) {
            case BLIND_BOX -> blindBoxService.cancel(goodsCancelSaleRequest);
            case COLLECTION -> collectionService.cancel(goodsCancelSaleRequest);
            case STAR -> starService.cancel(goodsCancelSaleRequest);
            default -> throw new UnsupportedOperationException(ERROR_CODE_UNSUPPORTED_GOODS_TYPE);
        };

        GoodsSaleResponse response = new GoodsSaleResponse();
        response.setSuccess(result);
        return response;
    }

    @Override
    public GoodsSaleResponse paySuccess(GoodsSaleRequest request) {
        GoodsSaleResponse response = new GoodsSaleResponse();
        GoodsType goodsType = GoodsType.valueOf(request.getGoodsType());

        return switch (goodsType) {
            case BLIND_BOX -> {
                BlindBoxAssignRequest blindBoxAssignRequest = new BlindBoxAssignRequest();
                blindBoxAssignRequest.setBlindBoxId(request.getGoodsId());
                blindBoxAssignRequest.setUserId(request.getUserId());
                blindBoxAssignRequest.setOrderId(request.getBizNo());
                blindBoxService.assign(blindBoxAssignRequest);
                response.setSuccess(true);
                yield response;
            }
            case COLLECTION -> {
                HeldCollectionCreateRequest heldCollectionCreateRequest = new HeldCollectionCreateRequest();
                BeanUtils.copyProperties(request, heldCollectionCreateRequest);
                heldCollectionCreateRequest.setReferencePrice(request.getPurchasePrice());
                heldCollectionCreateRequest.setSerialNoBaseId(request.getGoodsId().toString());
                // 从藏品主档带入稀有度/铸造值快照（queryById 走两级缓存）
                Collection collection = collectionService.queryById(request.getGoodsId());
                if (collection != null) {
                    heldCollectionCreateRequest.setRarity(collection.getRarity());
                    heldCollectionCreateRequest.setForgeValue(collection.getForgeValue());
                }

                HeldCollection heldCollection = heldCollectionService.create(heldCollectionCreateRequest);
                response.setSuccess(true);
                response.setHeldCollectionId(heldCollection.getId());
                yield response;
            }
            case STAR -> {
                // 星尘闪购：写库存流水 + 增加用户星尘余额
                starService.paySuccess(request.getGoodsId(), request.getUserId(),
                        request.getQuantity(), request.getBizNo());
                response.setSuccess(true);
                yield response;
            }
            default -> throw new UnsupportedOperationException(ERROR_CODE_UNSUPPORTED_GOODS_TYPE);
        };
    }

    @Override
    @Facade
    public Boolean addHotGoods(String goodsId, String goodsType) {
        hotGoodsService.addHotGoods(goodsId, goodsType);
        //不抛异常就视为成功
        return true;
    }

    @Override
    @Facade
    public Boolean isHotGoods(String goodsId, String goodsType) {
        return hotGoodsService.isHotGoods(goodsId, goodsType);
    }

    @Override
    @Facade
    public List<String> getHotGoods(String goodsType) {
        return hotGoodsService.getHotGoods(goodsType);
    }
}
