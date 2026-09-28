package cn.kaziki.nft.turbo.api.star.request;

import cn.kaziki.nft.turbo.base.request.PageRequest;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 星尘流水分页查询
 */
@Getter
@Setter
@ToString
public class StarStreamPageQueryRequest extends PageRequest {

    private static final long serialVersionUID = 1L;

    /** 用户ID（内部调用必传，HTTP接口从token获取） */
    private String userId;

    /** 开始日期（含，yyyy-MM-dd 格式，可选） */
    private String startDate;

    /** 结束日期（含，yyyy-MM-dd 格式，可选） */
    private String endDate;
}
