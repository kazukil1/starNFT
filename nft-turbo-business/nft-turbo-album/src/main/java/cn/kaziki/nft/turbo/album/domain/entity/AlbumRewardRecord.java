package cn.kaziki.nft.turbo.album.domain.entity;

import cn.kaziki.nft.turbo.datasource.domain.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 图鉴奖励领取记录（幂等防重）
 */
@Getter
@Setter
@TableName("album_reward_record")
public class AlbumRewardRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String userId;
    private Long seriesId;
    /** ALL_N / ALL_N_R / ALL_SR / ALL_UR */
    private String milestoneType;
    /** STAR / AIRDROP / BOTH */
    private String rewardType;
    private Long starAmount;
    private Long airdropCollectionId;
    private Long airdropHeldId;

    /** 领取星尘奖励 */
    public static AlbumRewardRecord createStarReward(
            String userId, Long seriesId, String milestoneType, Long starAmount) {
        AlbumRewardRecord record = new AlbumRewardRecord();
        record.setUserId(userId);
        record.setSeriesId(seriesId);
        record.setMilestoneType(milestoneType);
        record.setRewardType("STAR");
        record.setStarAmount(starAmount);
        return record;
    }

    /** 领取星尘+空投双奖励 */
    public static AlbumRewardRecord createBothReward(
            String userId, Long seriesId, String milestoneType, Long starAmount, Long airdropCollectionId) {
        AlbumRewardRecord record = createStarReward(userId, seriesId, milestoneType, starAmount);
        record.setRewardType("BOTH");
        record.setAirdropCollectionId(airdropCollectionId);
        return record;
    }
}
