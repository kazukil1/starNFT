package cn.kaziki.nft.turbo.api.album.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * 里程碑修改请求
 */
@Getter
@Setter
@ToString
public class AlbumMilestoneModifyRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "里程碑ID不能为空")
    private Long id;

    private String name;
    private Long starReward;
    private Long airdropCollectionId;
}