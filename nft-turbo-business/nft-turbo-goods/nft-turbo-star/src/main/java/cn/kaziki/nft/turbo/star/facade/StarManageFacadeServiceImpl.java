package cn.kaziki.nft.turbo.star.facade;

import cn.kaziki.nft.turbo.api.goods.constant.GoodsType;
import cn.kaziki.nft.turbo.api.inventory.request.InventoryRequest;
import cn.kaziki.nft.turbo.api.inventory.service.InventoryFacadeService;
import cn.kaziki.nft.turbo.api.star.request.StarCreateRequest;
import cn.kaziki.nft.turbo.api.star.request.StarModifyInventoryRequest;
import cn.kaziki.nft.turbo.api.star.request.StarModifyRequest;
import cn.kaziki.nft.turbo.api.star.request.StarRemoveRequest;
import cn.kaziki.nft.turbo.api.star.service.StarManageFacadeService;
import cn.kaziki.nft.turbo.base.response.SingleResponse;
import cn.kaziki.nft.turbo.rpc.facade.Facade;
import cn.kaziki.nft.turbo.star.domain.entity.StarDaily;
import cn.kaziki.nft.turbo.star.domain.service.StarService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

// 星尘闪购管理门面实现
@DubboService(version = "1.0.0")
public class StarManageFacadeServiceImpl implements StarManageFacadeService {

    @Autowired
    private StarService starService;

    @Autowired
    private InventoryFacadeService inventoryFacadeService;

    @Override
    @Facade
    public SingleResponse<Long> create(StarCreateRequest request) {
        // 1. DB 写入（本地事务，含流水）
        Long id = starService.create(request);
        // 2. Redis 初始化库存
        InventoryRequest invReq = new InventoryRequest();
        invReq.setGoodsId(id.toString());
        invReq.setGoodsType(GoodsType.STAR);
        invReq.setInventory(request.getQuantity());
        invReq.setIdentifier(request.getIdentifier());
        inventoryFacadeService.init(invReq);
        return SingleResponse.of(id);
    }

    @Override
    @Facade
    public SingleResponse<Long> modify(StarModifyRequest request) {
        return SingleResponse.of(starService.modify(request));
    }

    @Override
    @Facade
    public SingleResponse<Long> modifyInventory(StarModifyInventoryRequest request) {
        Long id = starService.modifyInventory(request);
        InventoryRequest invReq = new InventoryRequest();
        invReq.setGoodsId(request.getId().toString());
        invReq.setGoodsType(GoodsType.STAR);
        invReq.setInventory(request.getQuantityDelta());
        invReq.setIdentifier(request.getIdentifier());
        if (request.getQuantityDelta() >= 0) {
            inventoryFacadeService.increase(invReq);
        } else {
            inventoryFacadeService.decrease(invReq);
        }
        return SingleResponse.of(id);
    }

    @Override
    @Facade
    public SingleResponse<Boolean> remove(StarRemoveRequest request) {
        // 1. DB 更新
        Boolean result = starService.remove(request);
        // 2. 移除库存缓存
        if (result) {
            InventoryRequest invReq = new InventoryRequest();
            invReq.setGoodsId(request.getId().toString());
            invReq.setGoodsType(GoodsType.STAR);
            inventoryFacadeService.invalid(invReq);
        }
        return SingleResponse.of(result);
    }

    @Override
    public void initTodayInventory() {
        List<StarDaily> activeList = starService.initTodayInventory();
        for (StarDaily star : activeList) {
            InventoryRequest invReq = new InventoryRequest();
            invReq.setGoodsId(star.getId().toString());
            invReq.setGoodsType(GoodsType.STAR);
            invReq.setInventory(star.getQuantity());
            inventoryFacadeService.init(invReq);
        }
    }
}
