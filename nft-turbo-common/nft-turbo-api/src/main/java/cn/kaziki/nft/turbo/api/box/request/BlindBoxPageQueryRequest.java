package cn.kaziki.nft.turbo.api.box.request;

import cn.kaziki.nft.turbo.base.request.PageRequest;
import lombok.Getter;
import lombok.Setter;

/**
 *
 */
@Getter
@Setter
public class BlindBoxPageQueryRequest extends PageRequest {

    private String state;

    private String keyword;

    /** 创建者ID（Artist 只看自己的） */
    private String creatorId;

    /** 系列ID */
    private Long seriesId;
}
