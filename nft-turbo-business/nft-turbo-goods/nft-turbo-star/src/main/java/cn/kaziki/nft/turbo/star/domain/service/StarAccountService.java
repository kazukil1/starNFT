package cn.kaziki.nft.turbo.star.domain.service;

import cn.kaziki.nft.turbo.api.star.request.StarChangeRequest;
import cn.kaziki.nft.turbo.api.star.request.StarStreamPageQueryRequest;
import cn.kaziki.nft.turbo.base.response.PageResponse;
import cn.kaziki.nft.turbo.star.domain.entity.StarAccount;
import cn.kaziki.nft.turbo.star.domain.entity.StarAccountStream;

/**
 * 星尘账户领域服务
 */
public interface StarAccountService {

    /**
     * 初始化账户（存量用户首次操作，INSERT IGNORE）
     */
    StarAccount initAccount(String userId);

    /**
     * 查询账户
     */
    StarAccount getAccount(String userId);

    /**
     * 增加星尘（获得）
     */
    StarAccount increase(StarChangeRequest request);

    /**
     * 扣减星尘（消耗）
     */
    StarAccount decrease(StarChangeRequest request);

    /**
     * 流水分页查询
     */
    PageResponse<StarAccountStream> pageQueryStream(StarStreamPageQueryRequest request);
}
