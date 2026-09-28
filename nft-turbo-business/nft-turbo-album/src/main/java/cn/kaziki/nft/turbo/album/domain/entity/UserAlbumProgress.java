package cn.kaziki.nft.turbo.album.domain.entity;

import cn.kaziki.nft.turbo.datasource.domain.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * 用户图鉴进度快照
 */
@Getter
@Setter
@TableName("user_album_progress")
public class UserAlbumProgress extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String userId;
    private Long seriesId;
    /** 已点亮的 collectionId JSON 数组（逗号分隔） */
    private String collected;
    /** 系列藏品总数快照 */
    private Integer total;
    /** COLLECTING / COMPLETED */
    private String state;
    private Date completedAt;

    /** 存量用户首次访问：初始化进度 */
    public UserAlbumProgress init(String userId, Long seriesId, String collectedIds, int total) {
        this.userId = userId;
        this.seriesId = seriesId;
        this.collected = collectedIds;
        this.total = total;
        this.state = collectedIds != null && !collectedIds.isEmpty()
                ? (parseCount(collectedIds) >= total ? "COMPLETED" : "COLLECTING")
                : "COLLECTING";
        if ("COMPLETED".equals(this.state)) {
            this.completedAt = new Date();
        }
        return this;
    }

    /** 增量刷新进度（购买/开盒/合成/转赠触发） */
    public UserAlbumProgress refresh(String collectedIds, int total) {
        this.collected = collectedIds;
        this.total = total;
        this.state = parseCount(collectedIds) >= total ? "COMPLETED" : "COLLECTING";
        if ("COMPLETED".equals(this.state) && this.completedAt == null) {
            this.completedAt = new Date();
        }
        return this;
    }

    /** 标记失败 */
    public UserAlbumProgress fail() {
        this.state = "FAILED";
        return this;
    }

    private static int parseCount(String collected) {
        if (collected == null || collected.isBlank()) return 0;
        return (int) collected.chars().filter(c -> c == ',').count() + 1;
    }
}
