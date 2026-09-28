package cn.kaziki.nft.turbo.star.domain.service;

import cn.kaziki.nft.turbo.api.goods.request.GoodsCancelSaleRequest;
import cn.kaziki.nft.turbo.api.goods.request.GoodsTrySaleRequest;
import cn.kaziki.nft.turbo.api.star.request.StarCreateRequest;
import cn.kaziki.nft.turbo.api.star.request.StarModifyInventoryRequest;
import cn.kaziki.nft.turbo.api.star.request.StarModifyRequest;
import cn.kaziki.nft.turbo.api.star.request.StarPageQueryRequest;
import cn.kaziki.nft.turbo.api.star.request.StarRemoveRequest;
import cn.kaziki.nft.turbo.base.response.PageResponse;
import cn.kaziki.nft.turbo.star.domain.entity.StarDaily;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 星尘领域服务接口
 */
public interface StarService extends IService<StarDaily> {

    /** 分页查询 */
    PageResponse<StarDaily> pageQuery(StarPageQueryRequest request);

    /** 按 ID 查询（含缓存） */
    StarDaily queryById(Long id);

    /** 售卖（含售罄判断） */
    Boolean sale(GoodsTrySaleRequest request);

    /** 售卖（不抛售罄异常） */
    Boolean saleWithoutHint(GoodsTrySaleRequest request);

    /** 取消售卖 */
    Boolean cancel(GoodsCancelSaleRequest request);

    /** 支付成功：写流水 + 加余额 */
    void paySuccess(Long starId, String userId, Long quantity, String orderId);

    /** 创建星尘包 */
    Long create(StarCreateRequest request);

    /** 修改星尘包基本信息 */
    Long modify(StarModifyRequest request);

    /** 修改库存 */
    Long modifyInventory(StarModifyInventoryRequest request);

    /** 下架星尘包 */
    Boolean remove(StarRemoveRequest request);

    /** 每日库存初始化（XXL-Job） */
    List<StarDaily> initTodayInventory();
}
