package cn.kaziki.nft.turbo.api.album.service;

import cn.kaziki.nft.turbo.api.album.model.AlbumClaimResultVO;
import cn.kaziki.nft.turbo.api.album.model.AlbumMilestoneVO;
import cn.kaziki.nft.turbo.api.album.model.SeriesAlbumVO;
import cn.kaziki.nft.turbo.api.album.model.UserAlbumVO;
import cn.yueyu.nft.turbo.api.album.model.*;
import cn.kaziki.nft.turbo.api.album.request.AlbumClaimRequest;
import cn.kaziki.nft.turbo.api.album.request.AlbumMilestoneCreateRequest;
import cn.kaziki.nft.turbo.api.album.request.AlbumMilestoneModifyRequest;
import cn.kaziki.nft.turbo.base.response.PageResponse;
import cn.kaziki.nft.turbo.base.response.SingleResponse;

/**
 * 图鉴系统 Dubbo 门面接口
 */
public interface AlbumFacadeService {

    /** 用户全系列图鉴进度列表 */
    PageResponse<UserAlbumVO> pageQueryProgress(String userId);

    /** 单系列图鉴详情（含逐卡点亮状态） */
    SingleResponse<SeriesAlbumVO> getSeriesDetail(String userId, Long seriesId);

    /** 领取里程碑奖励（幂等） */
    SingleResponse<AlbumClaimResultVO> claimReward(AlbumClaimRequest request);

    /** 刷新用户在某系列的进度（内部触发，幂等） */
    SingleResponse<Boolean> refreshProgress(String userId, Long seriesId);

    // —— Admin ——

    /** 创建里程碑配置 */
    SingleResponse<AlbumMilestoneVO> createMilestone(AlbumMilestoneCreateRequest request);

    /** 修改里程碑配置 */
    SingleResponse<AlbumMilestoneVO> modifyMilestone(AlbumMilestoneModifyRequest request);

    /** 启用/停用里程碑 */
    SingleResponse<Boolean> toggleMilestone(Long id, String state);

    /** 系列里程碑列表 */
    PageResponse<AlbumMilestoneVO> pageQueryMilestone(Long seriesId, int currentPage, int pageSize);
}