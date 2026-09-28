package cn.kaziki.nft.turbo.album.domain.service;

import cn.kaziki.nft.turbo.album.domain.entity.AlbumMilestoneConfig;
import cn.kaziki.nft.turbo.album.domain.entity.UserAlbumProgress;
import cn.kaziki.nft.turbo.api.album.model.AlbumClaimResultVO;
import cn.kaziki.nft.turbo.api.album.model.SeriesAlbumVO;
import cn.kaziki.nft.turbo.api.album.model.UserAlbumVO;
import cn.yueyu.nft.turbo.api.album.model.*;
import cn.kaziki.nft.turbo.api.album.request.AlbumClaimRequest;
import cn.kaziki.nft.turbo.api.album.request.AlbumMilestoneCreateRequest;
import cn.kaziki.nft.turbo.api.album.request.AlbumMilestoneModifyRequest;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 图鉴领域服务
 */
public interface AlbumService extends IService<UserAlbumProgress> {

    /** 用户全系列图鉴进度列表（无记录时触发初始化） */
    List<UserAlbumVO> pageQueryProgress(String userId);

    /** 单系列图鉴详情（含逐卡点亮状态 + 里程碑状态） */
    SeriesAlbumVO getSeriesDetail(String userId, Long seriesId);

    /** 领取里程碑奖励（幂等） */
    AlbumClaimResultVO claimReward(AlbumClaimRequest request);

    /** 刷新用户在某系列的进度（幂等，事件触发） */
    void refreshProgress(String userId, Long seriesId);

    // —— Admin ——

    AlbumMilestoneConfig createMilestone(AlbumMilestoneCreateRequest request);
    AlbumMilestoneConfig modifyMilestone(AlbumMilestoneModifyRequest request);
    Boolean toggleMilestone(Long id, String state);
    List<AlbumMilestoneConfig> pageQueryMilestone(Long seriesId);
}