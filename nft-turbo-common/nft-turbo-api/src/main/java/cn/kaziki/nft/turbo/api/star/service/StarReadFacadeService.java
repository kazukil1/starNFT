package cn.kaziki.nft.turbo.api.star.service;

import cn.kaziki.nft.turbo.api.star.model.StarVO;
import cn.kaziki.nft.turbo.api.star.request.StarPageQueryRequest;
import cn.kaziki.nft.turbo.base.response.PageResponse;
import cn.kaziki.nft.turbo.base.response.SingleResponse;

/**
 * 星尘读取门面服务（Dubbo 接口）
 */
public interface StarReadFacadeService {

    // 分页查询
    PageResponse<StarVO> pageQuery(StarPageQueryRequest request);

    // 根据 ID 查询
    SingleResponse<StarVO> queryById(Long id);
}
