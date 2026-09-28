package cn.kaziki.nft.turbo.api.album.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * 里程碑创建请求
 */
@Getter
@Setter
@ToString
public class AlbumMilestoneCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "系列ID不能为空")
    private Long seriesId;

    @NotBlank(message = "里程碑名称不能为空")
    private String name;

    @NotBlank(message = "里程碑类型不能为空")
    private String milestoneType;

    @NotNull(message = "需收集卡数不能为空")
    private Integer requiredCount;

    private Long starReward;
    private Long airdropCollectionId;
}