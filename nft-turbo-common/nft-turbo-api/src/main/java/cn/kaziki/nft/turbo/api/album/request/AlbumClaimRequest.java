package cn.kaziki.nft.turbo.api.album.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * 里程碑奖励领取请求
 */
@Getter
@Setter
@ToString
public class AlbumClaimRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID（由 Controller 注入） */
    private String userId;

    @NotNull(message = "系列ID不能为空")
    private Long seriesId;

    @NotBlank(message = "里程碑类型不能为空")
    private String milestoneType;
}