package cn.kaziki.nft.turbo.album.facade;

import cn.kaziki.nft.turbo.album.domain.entity.AlbumMilestoneConfig;
import cn.kaziki.nft.turbo.album.domain.service.AlbumService;
import cn.kaziki.nft.turbo.api.album.model.AlbumClaimResultVO;
import cn.kaziki.nft.turbo.api.album.model.AlbumMilestoneVO;
import cn.kaziki.nft.turbo.api.album.model.SeriesAlbumVO;
import cn.kaziki.nft.turbo.api.album.model.UserAlbumVO;
import cn.kaziki.nft.turbo.api.album.request.AlbumClaimRequest;
import cn.kaziki.nft.turbo.api.album.request.AlbumMilestoneCreateRequest;
import cn.kaziki.nft.turbo.api.album.request.AlbumMilestoneModifyRequest;
import cn.kaziki.nft.turbo.api.album.service.AlbumFacadeService;
import cn.kaziki.nft.turbo.base.response.PageResponse;
import cn.kaziki.nft.turbo.base.response.SingleResponse;
import cn.kaziki.nft.turbo.rpc.facade.Facade;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 图鉴系统 Dubbo 门面
 */
@DubboService(version = "1.0.0")
public class AlbumFacadeServiceImpl implements AlbumFacadeService {

    @Autowired
    private AlbumService albumService;

    @Override
    @Facade
    public PageResponse<UserAlbumVO> pageQueryProgress(String userId) {
        List<UserAlbumVO> list = albumService.pageQueryProgress(userId);
        return PageResponse.of(list, list.size(), 20, 1);
    }

    @Override
    @Facade
    public SingleResponse<SeriesAlbumVO> getSeriesDetail(String userId, Long seriesId) {
        SeriesAlbumVO vo = albumService.getSeriesDetail(userId, seriesId);
        return SingleResponse.of(vo);
    }

    @Override
    @Facade
    public SingleResponse<AlbumClaimResultVO> claimReward(AlbumClaimRequest request) {
        AlbumClaimResultVO result = albumService.claimReward(request);
        return SingleResponse.of(result);
    }

    @Override
    public SingleResponse<Boolean> refreshProgress(String userId, Long seriesId) {
        albumService.refreshProgress(userId, seriesId);
        return SingleResponse.of(true);
    }

    @Override
    @Facade
    public SingleResponse<AlbumMilestoneVO> createMilestone(AlbumMilestoneCreateRequest request) {
        AlbumMilestoneConfig config = albumService.createMilestone(request);
        return SingleResponse.of(toMilestoneVO(config));
    }

    @Override
    @Facade
    public SingleResponse<AlbumMilestoneVO> modifyMilestone(AlbumMilestoneModifyRequest request) {
        AlbumMilestoneConfig config = albumService.modifyMilestone(request);
        return SingleResponse.of(toMilestoneVO(config));
    }

    @Override
    @Facade
    public SingleResponse<Boolean> toggleMilestone(Long id, String state) {
        return SingleResponse.of(albumService.toggleMilestone(id, state));
    }

    @Override
    public PageResponse<AlbumMilestoneVO> pageQueryMilestone(Long seriesId, int currentPage, int pageSize) {
        List<AlbumMilestoneConfig> list = albumService.pageQueryMilestone(seriesId);
        List<AlbumMilestoneVO> vos = list.stream().map(this::toMilestoneVO).collect(Collectors.toList());
        return PageResponse.of(vos, vos.size(), pageSize, currentPage);
    }

    private AlbumMilestoneVO toMilestoneVO(AlbumMilestoneConfig c) {
        AlbumMilestoneVO vo = new AlbumMilestoneVO();
        vo.setId(c.getId());
        vo.setSeriesId(c.getSeriesId());
        vo.setName(c.getName());
        vo.setMilestoneType(c.getMilestoneType());
        vo.setRequiredCount(c.getRequiredCount());
        vo.setStarReward(c.getStarReward());
        vo.setAirdropCollectionId(c.getAirdropCollectionId());
        vo.setState(c.getState());
        return vo;
    }
}
