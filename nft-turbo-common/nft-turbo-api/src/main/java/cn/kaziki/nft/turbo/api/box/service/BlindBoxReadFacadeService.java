package cn.kaziki.nft.turbo.api.box.service;

import cn.kaziki.nft.turbo.api.box.model.BlindBoxItemVO;
import cn.kaziki.nft.turbo.api.box.model.BlindBoxVO;
import cn.kaziki.nft.turbo.api.box.model.HeldBlindBoxVO;
import cn.kaziki.nft.turbo.api.box.request.BlindBoxItemPageQueryRequest;
import cn.kaziki.nft.turbo.api.box.request.BlindBoxPageQueryRequest;
import cn.kaziki.nft.turbo.base.response.PageResponse;
import cn.kaziki.nft.turbo.base.response.SingleResponse;

/**
 * 盲盒门面 服务
 */
public interface BlindBoxReadFacadeService {

    /**
     * 根据Id查询藏品
     *
     * @param blindBoxId
     * @return
     */
    SingleResponse<BlindBoxVO> queryById(Long blindBoxId);

    /**
     * 根据id查询盲盒条目
     *
     * @param blindBoxItemId
     * @return
     */
    SingleResponse<BlindBoxItemVO> queryBlindBoxItemById(Long blindBoxItemId);

    /**
     * 盲盒分页查询
     *
     * @param request
     * @return
     */
    public PageResponse<BlindBoxVO> pageQueryBlindBox(BlindBoxPageQueryRequest request);

    /**
     * 盲盒条目分页查询
     *
     * @param request
     * @return
     */
    public PageResponse<HeldBlindBoxVO> pageQueryBlindBoxItem(BlindBoxItemPageQueryRequest request);
}
