package cn.kaziki.nft.turbo.api.collection.service;

import cn.kaziki.nft.turbo.api.collection.model.SeriesVO;
import cn.kaziki.nft.turbo.api.collection.request.SeriesPageQueryRequest;
import cn.kaziki.nft.turbo.base.response.PageResponse;
import cn.kaziki.nft.turbo.base.response.SingleResponse;

/**
 * 藏品系列读门面服务
 */
public interface SeriesReadFacadeService {

    // 根据id查询系列
    SingleResponse<SeriesVO> queryById(Long seriesId);

    // 系列分页查询
    PageResponse<SeriesVO> pageQuery(SeriesPageQueryRequest request);
}
