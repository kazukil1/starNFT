package cn.kaziki.nft.turbo.album.domain.service.impl;

import cn.kaziki.nft.turbo.album.domain.entity.AlbumMilestoneConfig;
import cn.kaziki.nft.turbo.album.domain.entity.AlbumRewardRecord;
import cn.kaziki.nft.turbo.album.domain.entity.UserAlbumProgress;
import cn.kaziki.nft.turbo.album.domain.service.AlbumService;
import cn.kaziki.nft.turbo.album.exception.AlbumException;
import cn.kaziki.nft.turbo.album.infrastructure.mapper.AlbumMilestoneConfigMapper;
import cn.kaziki.nft.turbo.album.infrastructure.mapper.AlbumRewardRecordMapper;
import cn.kaziki.nft.turbo.album.infrastructure.mapper.UserAlbumProgressMapper;
import cn.kaziki.nft.turbo.api.album.model.*;
import cn.yueyu.nft.turbo.api.album.model.*;
import cn.kaziki.nft.turbo.api.album.request.AlbumClaimRequest;
import cn.kaziki.nft.turbo.api.album.request.AlbumMilestoneCreateRequest;
import cn.kaziki.nft.turbo.api.album.request.AlbumMilestoneModifyRequest;
import cn.kaziki.nft.turbo.api.collection.constant.CollectionStateEnum;
import cn.kaziki.nft.turbo.api.collection.constant.SeriesStateEnum;
import cn.kaziki.nft.turbo.api.collection.model.CollectionVO;
import cn.kaziki.nft.turbo.api.collection.model.HeldCollectionVO;
import cn.kaziki.nft.turbo.api.collection.model.SeriesVO;
import cn.kaziki.nft.turbo.api.collection.request.SeriesPageQueryRequest;
import cn.kaziki.nft.turbo.api.collection.service.CollectionReadFacadeService;
import cn.kaziki.nft.turbo.api.collection.service.SeriesReadFacadeService;
import cn.kaziki.nft.turbo.api.star.constant.StarChangeType;
import cn.kaziki.nft.turbo.api.star.request.StarChangeRequest;
import cn.kaziki.nft.turbo.api.star.service.StarAccountFacadeService;
import cn.kaziki.nft.turbo.base.response.PageResponse;
import cn.kaziki.nft.turbo.base.response.SingleResponse;
import cn.hutool.core.lang.Assert;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static cn.kaziki.nft.turbo.album.exception.AlbumErrorCode.*;

/**
 * 图鉴领域服务
 */
@Slf4j
@Service
public class AlbumServiceImpl extends ServiceImpl<UserAlbumProgressMapper, UserAlbumProgress>
        implements AlbumService {

    @Autowired
    private UserAlbumProgressMapper progressMapper;
    @Autowired
    private AlbumMilestoneConfigMapper milestoneMapper;
    @Autowired
    private AlbumRewardRecordMapper rewardRecordMapper;
    @Autowired
    private SeriesReadFacadeService seriesReadFacadeService;
    @Autowired
    private CollectionReadFacadeService collectionReadFacadeService;
    @Autowired
    private StarAccountFacadeService starAccountFacadeService;

    // ==================== 进度查询 ====================

    @Override
    public List<UserAlbumVO> pageQueryProgress(String userId) {
        // 1. 查所有 SUCCEED 系列
        SeriesPageQueryRequest seriesReq = new SeriesPageQueryRequest();
        seriesReq.setState(SeriesStateEnum.SUCCEED);
        seriesReq.setCurrentPage(1);
        seriesReq.setPageSize(1000);
        PageResponse<SeriesVO> seriesPage = seriesReadFacadeService.pageQuery(seriesReq);
        List<SeriesVO> allSeries = seriesPage.getDatas();
        if (allSeries.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 存量用户首次访问：全量初始化
        List<UserAlbumProgress> progressList = list(
                new QueryWrapper<UserAlbumProgress>().eq("user_id", userId));
        if (progressList.isEmpty()) {
            log.info("存量用户首次访问图鉴，触发全量初始化，userId={}", userId);
            for (SeriesVO s : allSeries) {
                initProgressForSeries(userId, s.getId());
            }
            progressList = list(new QueryWrapper<UserAlbumProgress>().eq("user_id", userId));
        }

        // 3. 组装 VO
        Map<Long, UserAlbumProgress> progressMap = progressList.stream()
                .collect(Collectors.toMap(UserAlbumProgress::getSeriesId, p -> p));

        return allSeries.stream().map(s -> {
            UserAlbumVO vo = new UserAlbumVO();
            vo.setSeriesId(s.getId());
            vo.setSeriesName(s.getName());
            vo.setSeriesCover(s.getCover());
            UserAlbumProgress p = progressMap.get(s.getId());
            if (p != null) {
                vo.setTotal(p.getTotal());
                vo.setCollected(p.getCollected() != null
                        ? parseCollectedIds(p.getCollected()).size() : 0);
                vo.setState(p.getState());
                vo.setCompletedAt(p.getCompletedAt());
            } else {
                vo.setTotal(0);
                vo.setCollected(0);
                vo.setState("COLLECTING");
            }
            return vo;
        }).collect(Collectors.toList());
    }

    /** 单系列进度初始化（幂等 insert） */
    private void initProgressForSeries(String userId, Long seriesId) {
        Set<Long> everHeldIds = queryEverHeldCollectionIds(userId, seriesId);
        List<CollectionVO> allInSeries = getAllCollectionsInSeries(seriesId);
        String collectedIds = everHeldIds.stream().sorted()
                .map(String::valueOf).collect(Collectors.joining(","));
        UserAlbumProgress progress = new UserAlbumProgress()
                .init(userId, seriesId, collectedIds, allInSeries.size());
        progressMapper.insert(progress);
    }

    @Override
    public SeriesAlbumVO getSeriesDetail(String userId, Long seriesId) {
        // 1. 系列信息
        SingleResponse<SeriesVO> seriesResp = seriesReadFacadeService.queryById(seriesId);
        Assert.isTrue(seriesResp.getSuccess() && seriesResp.getData() != null,
                () -> new AlbumException(SERIES_NOT_FOUND));
        SeriesVO series = seriesResp.getData();

        // 2. 全部藏品 + 用户进度
        List<CollectionVO> allCollections = getAllCollectionsInSeries(seriesId);
        UserAlbumProgress progress = progressMapper.selectByUserAndSeries(userId, seriesId);
        Set<Long> collectedIds = progress != null && progress.getCollected() != null
                ? parseCollectedIds(progress.getCollected()) : Collections.emptySet();

        // 3. 当前持有
        SingleResponse<List<HeldCollectionVO>> holdingsResp =
                collectionReadFacadeService.listHeldCollectionsByUser(userId);
        final Set<Long> holdingIds;
        if (holdingsResp.getSuccess() && holdingsResp.getData() != null) {
            holdingIds = holdingsResp.getData().stream()
                    .filter(h -> "ACTIVED".equals(h.getState()))
                    .map(HeldCollectionVO::getCollectionId)
                    .collect(Collectors.toSet());
        } else {
            holdingIds = Collections.emptySet();
        }

        // 4. 组装 VO
        SeriesAlbumVO vo = new SeriesAlbumVO();
        vo.setSeriesId(series.getId());
        vo.setSeriesName(series.getName());
        vo.setSeriesCover(series.getCover());
        vo.setTotal(allCollections.size());
        vo.setCollected(collectedIds.size());
        vo.setState(progress != null ? progress.getState() : "COLLECTING");
        vo.setCompletedAt(progress != null ? progress.getCompletedAt() : null);

        // 5. 逐卡信息
        List<AlbumCardVO> cards = allCollections.stream().map(c -> {
            AlbumCardVO card = new AlbumCardVO();
            card.setCollectionId(c.getId());
            card.setName(c.getName());
            card.setCover(c.getCover());
            card.setRarity(c.getRarity() != null ? c.getRarity().name() : null);
            card.setOwned(holdingIds.contains(c.getId()));
            card.setEverHad(collectedIds.contains(c.getId()));
            return card;
        }).collect(Collectors.toList());
        vo.setCards(cards);

        // 6. 里程碑状态
        List<AlbumMilestoneConfig> milestones = milestoneMapper.selectList(
                new QueryWrapper<AlbumMilestoneConfig>()
                        .eq("series_id", seriesId).eq("state", "ACTIVE"));
        List<AlbumMilestoneVO> milestoneVOs = milestones.stream().map(m -> {
            AlbumMilestoneVO mvo = new AlbumMilestoneVO();
            mvo.setId(m.getId());
            mvo.setSeriesId(m.getSeriesId());
            mvo.setName(m.getName());
            mvo.setMilestoneType(m.getMilestoneType());
            mvo.setRequiredCount(m.getRequiredCount());
            mvo.setStarReward(m.getStarReward());
            mvo.setAirdropCollectionId(m.getAirdropCollectionId());
            mvo.setState(m.getState());
            mvo.setCompleted(collectedIds.size() >= m.getRequiredCount());
            boolean claimed = rewardRecordMapper.selectCount(
                    new QueryWrapper<AlbumRewardRecord>()
                            .eq("user_id", userId)
                            .eq("series_id", seriesId)
                            .eq("milestone_type", m.getMilestoneType())) > 0;
            mvo.setClaimed(claimed);
            return mvo;
        }).collect(Collectors.toList());
        vo.setMilestones(milestoneVOs);

        return vo;
    }

    // ==================== 奖励领取 ====================

    @Transactional(rollbackFor = Exception.class)
    @Override
    public AlbumClaimResultVO claimReward(AlbumClaimRequest request) {
        String userId = request.getUserId();
        Long seriesId = request.getSeriesId();
        String milestoneType = request.getMilestoneType();

        // 1. 查里程碑配置
        AlbumMilestoneConfig milestone = milestoneMapper.selectOne(
                new QueryWrapper<AlbumMilestoneConfig>()
                        .eq("series_id", seriesId)
                        .eq("milestone_type", milestoneType)
                        .eq("state", "ACTIVE"));
        Assert.notNull(milestone, () -> new AlbumException(MILESTONE_NOT_FOUND));

        // 2. 幂等：是否已领取
        Long claimedCount = rewardRecordMapper.selectCount(
                new QueryWrapper<AlbumRewardRecord>()
                        .eq("user_id", userId)
                        .eq("series_id", seriesId)
                        .eq("milestone_type", milestoneType));
        Assert.isTrue(claimedCount == 0, () -> new AlbumException(ALREADY_CLAIMED));

        // 3. 校验进度是否达成
        UserAlbumProgress progress = progressMapper.selectByUserAndSeries(userId, seriesId);
        Set<Long> collectedIds = progress != null && progress.getCollected() != null
                ? parseCollectedIds(progress.getCollected()) : Collections.emptySet();
        Assert.isTrue(collectedIds.size() >= milestone.getRequiredCount(),
                () -> new AlbumException(MILESTONE_NOT_COMPLETED));

        // 4. 发星尘奖励
        AlbumClaimResultVO result = new AlbumClaimResultVO();
        result.setSuccess(true);
        if (milestone.getStarReward() != null && milestone.getStarReward() > 0) {
            StarChangeRequest starReq = new StarChangeRequest();
            starReq.setUserId(userId);
            starReq.setAmount(milestone.getStarReward());
            starReq.setChangeType(StarChangeType.ALBUM_REWARD);
            starReq.setBizNo(userId + "_" + seriesId + "_" + milestoneType);
            starReq.setIdentifier(userId + "_ALBUM_" + seriesId + "_" + milestoneType);
            starAccountFacadeService.increase(starReq);
            result.setStarRewarded(milestone.getStarReward());
        }

        // 5. 发隐藏款空投（TODO）
        boolean hasAirDrop = milestone.getAirdropCollectionId() != null;
        if (hasAirDrop) {
            result.setAirdropHeldId(null);
        }

        // 6. 写领取记录
        AlbumRewardRecord record = hasAirDrop
                ? AlbumRewardRecord.createBothReward(userId, seriesId, milestoneType,
                        milestone.getStarReward(), milestone.getAirdropCollectionId())
                : AlbumRewardRecord.createStarReward(userId, seriesId, milestoneType,
                        milestone.getStarReward());
        var insertResult = rewardRecordMapper.insert(record);
        Assert.isTrue(insertResult > 0, () -> new AlbumException(REWARD_SAVE_FAILED));

        return result;
    }

    // ==================== 进度刷新 ====================

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void refreshProgress(String userId, Long seriesId) {
        Set<Long> everHeldIds = queryEverHeldCollectionIds(userId, seriesId);
        long total = getAllCollectionsInSeries(seriesId).size();
        String collectedIds = everHeldIds.stream().sorted()
                .map(String::valueOf).collect(Collectors.joining(","));

        UserAlbumProgress progress = progressMapper.selectByUserAndSeries(userId, seriesId);
        if (progress == null) {
            progress = new UserAlbumProgress().init(userId, seriesId, collectedIds, (int) total);
            var insertResult = progressMapper.insert(progress);
            Assert.isTrue(insertResult > 0, () -> new AlbumException(PROGRESS_SAVE_FAILED));
        } else {
            progress.refresh(collectedIds, (int) total);
            var updateResult = progressMapper.updateById(progress);
            Assert.isTrue(updateResult > 0, () -> new AlbumException(PROGRESS_SAVE_FAILED));
        }
    }

    // ==================== Admin ====================

    @Transactional(rollbackFor = Exception.class)
    @Override
    public AlbumMilestoneConfig createMilestone(AlbumMilestoneCreateRequest request) {
        AlbumMilestoneConfig config = AlbumMilestoneConfig.create(request);
        var result = milestoneMapper.insert(config);
        Assert.isTrue(result > 0, () -> new AlbumException(MILESTONE_SAVE_FAILED));
        return config;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public AlbumMilestoneConfig modifyMilestone(AlbumMilestoneModifyRequest request) {
        AlbumMilestoneConfig config = milestoneMapper.selectById(request.getId());
        Assert.notNull(config, () -> new AlbumException(MILESTONE_NOT_FOUND));
        config.modify(request);
        var result = milestoneMapper.updateById(config);
        Assert.isTrue(result > 0, () -> new AlbumException(MILESTONE_SAVE_FAILED));
        return config;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean toggleMilestone(Long id, String state) {
        AlbumMilestoneConfig config = milestoneMapper.selectById(id);
        Assert.notNull(config, () -> new AlbumException(MILESTONE_NOT_FOUND));
        if ("ACTIVE".equals(state)) {
            config.enable();
        } else {
            config.disable();
        }
        var result = milestoneMapper.updateById(config);
        Assert.isTrue(result > 0, () -> new AlbumException(MILESTONE_SAVE_FAILED));
        return true;
    }

    @Override
    public List<AlbumMilestoneConfig> pageQueryMilestone(Long seriesId) {
        QueryWrapper<AlbumMilestoneConfig> wrapper = new QueryWrapper<>();
        if (seriesId != null) {
            wrapper.eq("series_id", seriesId);
        }
        return milestoneMapper.selectList(wrapper);
    }

    // ==================== 内部方法 ====================

    /** 查询用户在某系列"曾经持有"的全部 collectionId */
    private Set<Long> queryEverHeldCollectionIds(String userId, Long seriesId) {
        SingleResponse<List<HeldCollectionVO>> resp =
                collectionReadFacadeService.listHeldCollectionsByUser(userId);
        if (!resp.getSuccess() || resp.getData() == null) {
            return Collections.emptySet();
        }
        return resp.getData().stream()
                .map(HeldCollectionVO::getCollectionId)
                .collect(Collectors.toSet());
    }

    /** 查询系列内全部 SUCCEED 状态的藏品 */
    private List<CollectionVO> getAllCollectionsInSeries(Long seriesId) {
        SingleResponse<List<CollectionVO>> resp =
                collectionReadFacadeService.listCollectionsBySeries(
                        seriesId, CollectionStateEnum.SUCCEED.name());
        if (!resp.getSuccess() || resp.getData() == null) {
            return Collections.emptyList();
        }
        return resp.getData();
    }

    /** 解析 collected JSON 数组 */
    private Set<Long> parseCollectedIds(String collected) {
        if (collected == null || collected.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(collected.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toSet());
    }
}
