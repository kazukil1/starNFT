package cn.kaziki.nft.turbo.api.star.request;

import cn.kaziki.nft.turbo.base.request.PageRequest;
import lombok.Getter;
import lombok.Setter;

/**
 * 星尘包分页查询请求
 */
@Getter
@Setter
public class StarPageQueryRequest extends PageRequest {

    private static final long serialVersionUID = 1L;

    private String state;
    private String keyword;
}
