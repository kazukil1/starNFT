package cn.kaziki.nft.turbo.api.collection.service;

import cn.kaziki.nft.turbo.api.collection.model.SeriesVO;
import cn.kaziki.nft.turbo.api.collection.request.SeriesCreateRequest;
import cn.kaziki.nft.turbo.api.collection.request.SeriesModifyRequest;
import cn.kaziki.nft.turbo.api.collection.request.SeriesRemoveRequest;
import cn.kaziki.nft.turbo.base.response.SingleResponse;

/**
 * 系列管理门面服务
 */
public interface SeriesManageFacadeService {

    // 创建系列，返回系列ID
    SingleResponse<Long> createSeries(SeriesCreateRequest request);

    // 修改系列（含状态流转）
    SingleResponse<SeriesVO> modifySeries(SeriesModifyRequest request);

    // 下架系列
    SingleResponse<Long> remove(SeriesRemoveRequest request);
}
