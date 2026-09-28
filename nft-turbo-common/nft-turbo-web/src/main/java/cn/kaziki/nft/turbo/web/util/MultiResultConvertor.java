package cn.kaziki.nft.turbo.web.util;

import cn.kaziki.nft.turbo.base.response.PageResponse;
import cn.kaziki.nft.turbo.web.vo.MultiResult;

import static cn.kaziki.nft.turbo.base.response.ResponseCode.SUCCESS;

/**
 * @author Hollis
 */
public class MultiResultConvertor {

    public static <T> MultiResult<T> convert(PageResponse<T> pageResponse) {
        MultiResult<T> multiResult = new MultiResult<T>(true, SUCCESS.name(), SUCCESS.name(), pageResponse.getDatas(), pageResponse.getTotal(), pageResponse.getCurrentPage(), pageResponse.getPageSize());
        return multiResult;
    }
}
