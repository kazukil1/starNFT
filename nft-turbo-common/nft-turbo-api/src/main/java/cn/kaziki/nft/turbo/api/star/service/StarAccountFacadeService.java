package cn.kaziki.nft.turbo.api.star.service;

import cn.kaziki.nft.turbo.api.star.model.StarAccountVO;
import cn.kaziki.nft.turbo.api.star.model.StarStreamVO;
import cn.kaziki.nft.turbo.api.star.request.StarChangeRequest;
import cn.kaziki.nft.turbo.api.star.request.StarStreamPageQueryRequest;
import cn.kaziki.nft.turbo.base.response.PageResponse;
import cn.kaziki.nft.turbo.base.response.SingleResponse;

/**
 * 星尘账户门面服务
 */
public interface StarAccountFacadeService {

    /**
     * 查询余额
     *
     * @param userId 用户ID
     * @return 当前星尘余额
     */
    SingleResponse<Long> getBalance(String userId);

    /**
     * 查询账户信息（余额 + 累计 + 状态）
     *
     * @param userId 用户ID
     * @return 星尘账户 VO
     */
    SingleResponse<StarAccountVO> getAccountInfo(String userId);

    /**
     * 增加星尘（获得）
     *
     * @param request 变更请求
     * @return 变更后余额
     */
    SingleResponse<Long> increase(StarChangeRequest request);

    /**
     * 扣减星尘（消耗）
     *
     * @param request 变更请求
     * @return 变更后余额
     */
    SingleResponse<Long> decrease(StarChangeRequest request);

    /**
     * 初始化星尘账户（用户注册时调用）
     *
     * @param userId 用户ID
     * @return 是否成功
     */
    SingleResponse<Boolean> initAccount(String userId);

    /**
     * 流水分页查询
     *
     * @param request 查询请求
     * @return 分页流水
     */
    PageResponse<StarStreamVO> pageQueryStream(StarStreamPageQueryRequest request);
}
