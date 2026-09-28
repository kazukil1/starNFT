package cn.kaziki.nft.turbo.box.domain.service;

import cn.kaziki.nft.turbo.api.box.request.BlindBoxCreateRequest;
import cn.kaziki.nft.turbo.api.box.request.BlindBoxModifyInventoryRequest;
import cn.kaziki.nft.turbo.api.goods.request.*;
import cn.yueyu.nft.turbo.api.goods.request.*;
import cn.kaziki.nft.turbo.base.response.PageResponse;
import cn.kaziki.nft.turbo.box.domain.entity.BlindBox;
import cn.kaziki.nft.turbo.box.domain.request.BlindBoxAssignRequest;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 盲盒服务
 */
public interface BlindBoxService extends IService<BlindBox> {
    // 创建
    public BlindBox create(BlindBoxCreateRequest request);

    // 售卖
    public Boolean sale(GoodsTrySaleRequest request);

    // 售卖-无hint版
    public Boolean saleWithoutHint(GoodsTrySaleRequest request);

    // 冻结库存
    public Boolean freezeInventory(GoodsFreezeInventoryRequest request);

    // 解冻库存并售卖
    public Boolean unfreezeAndSale(GoodsUnfreezeAndSaleRequest request);


    // 解冻库存
    public Boolean unfreezeInventory(GoodsUnfreezeInventoryRequest request);

    // 盲盒分配
    public Boolean assign(BlindBoxAssignRequest request);


    // 取消售卖
    public Boolean cancel(GoodsCancelSaleRequest request);

    // 补充库存
    public Long modifyInventory(BlindBoxModifyInventoryRequest request);

    // 查询
    public BlindBox queryById(Long blindBoxId);

    // 分页查询
    public PageResponse<BlindBox> pageQueryByState(String keyword, String state, String creatorId, Long seriesId, int currentPage, int pageSize);
}
