package cn.kaziki.nft.turbo.order.validator;

import cn.kaziki.nft.turbo.api.goods.model.BaseGoodsVO;
import cn.kaziki.nft.turbo.api.goods.service.GoodsFacadeService;
import cn.kaziki.nft.turbo.api.order.request.OrderCreateRequest;
import cn.kaziki.nft.turbo.order.OrderException;

import static cn.kaziki.nft.turbo.api.order.constant.OrderErrorCode.GOODS_NOT_BOOKED;

/**
 * 商品预约校验器
 */
public class GoodsBookValidator extends BaseOrderCreateValidator {

    private GoodsFacadeService goodsFacadeService;

    @Override
    protected void doValidate(OrderCreateRequest request) throws OrderException {
        BaseGoodsVO baseGoodsVO = goodsFacadeService.getGoods(request.getGoodsId(), request.getGoodsType());
        if(baseGoodsVO.canBook()){
            Boolean hasBooked = goodsFacadeService.isGoodsBooked(request.getGoodsId(), request.getGoodsType(), request.getBuyerId());

            if (!hasBooked) {
                throw new OrderException(GOODS_NOT_BOOKED);
            }
        }
    }

    public GoodsBookValidator(GoodsFacadeService goodsFacadeService) {
        this.goodsFacadeService = goodsFacadeService;
    }

    public GoodsBookValidator() {
    }
}
