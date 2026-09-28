package cn.kaziki.nft.turbo.star.facade;

import cn.kaziki.nft.turbo.api.goods.constant.GoodsState;
import cn.kaziki.nft.turbo.api.goods.constant.GoodsType;
import cn.kaziki.nft.turbo.api.inventory.request.InventoryRequest;
import cn.kaziki.nft.turbo.api.inventory.service.InventoryFacadeService;
import cn.kaziki.nft.turbo.api.star.model.StarVO;
import cn.kaziki.nft.turbo.api.star.request.StarPageQueryRequest;
import cn.kaziki.nft.turbo.api.star.service.StarReadFacadeService;
import cn.kaziki.nft.turbo.base.response.PageResponse;
import cn.kaziki.nft.turbo.base.response.SingleResponse;
import cn.kaziki.nft.turbo.rpc.facade.Facade;
import cn.kaziki.nft.turbo.star.domain.entity.StarDaily;
import cn.kaziki.nft.turbo.star.domain.entity.convertor.StarConvertor;
import cn.kaziki.nft.turbo.star.domain.service.StarService;
import java.util.List;

import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

import static cn.kaziki.nft.turbo.star.exception.StarErrorCode.STAR_NOT_EXIST;

// 星尘读取门面实现（Dubbo 暴露，供 C 端和 Admin 调用）
@DubboService(version = "1.0.0")
public class StarReadFacadeServiceImpl implements StarReadFacadeService {

    @Autowired
    private StarService starService;

    @Autowired
    private InventoryFacadeService inventoryFacadeService;

    @Override
    public PageResponse<StarVO> pageQuery(StarPageQueryRequest request) {
        PageResponse<StarDaily> page = starService.pageQuery(request);
        List<StarVO> voList = StarConvertor.INSTANCE.mapToVo(page.getDatas());
        for (StarVO vo : voList) {
            fillRemainingInventory(vo);
        }
        return PageResponse.of(voList, page.getTotal(), page.getPageSize(), page.getCurrentPage());
    }

    @Override
    @Facade
    public SingleResponse<StarVO> queryById(Long id) {
        // 1.查实体
        StarDaily star = starService.queryById(id);
        if (star == null) {
            return SingleResponse.fail(STAR_NOT_EXIST.getCode(),
                    STAR_NOT_EXIST.getMessage());
        }
        // 2.查实时库存（Redis）
        InventoryRequest request = new InventoryRequest();
        request.setGoodsId(id.toString());
        request.setGoodsType(GoodsType.STAR);
        SingleResponse<Integer> response = inventoryFacadeService.queryInventory(request);
        Long inventory = star.getSaleableInventory();
        if (response.getSuccess() && response.getData() != null) {
            inventory = Long.valueOf(response.getData());
        }
        // 3.组装 VO
        StarVO starVO = StarConvertor.INSTANCE.mapToVo(star);
        starVO.setRemainingInventory(inventory);
        starVO.setState(inventory > 0 ? GoodsState.SELLING : GoodsState.SOLD_OUT);
        return SingleResponse.of(starVO);
    }

    // 从 Redis 查实时库存
    private void fillRemainingInventory(StarVO vo) {
        InventoryRequest invReq = new InventoryRequest();
        invReq.setGoodsId(vo.getId().toString());
        invReq.setGoodsType(GoodsType.STAR);
        var response = inventoryFacadeService.queryInventory(invReq);
        long remaining = (response.getSuccess() && response.getData() != null)
                ? Long.valueOf(response.getData()) : 0L;
        vo.setRemainingInventory(remaining);
        vo.setState(remaining > 0 ? GoodsState.SELLING : GoodsState.SOLD_OUT);
    }
}
