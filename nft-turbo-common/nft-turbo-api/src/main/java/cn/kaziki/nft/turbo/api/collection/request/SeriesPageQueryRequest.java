package cn.kaziki.nft.turbo.api.collection.request;

import cn.kaziki.nft.turbo.api.collection.constant.SeriesStateEnum;
import cn.kaziki.nft.turbo.base.request.PageRequest;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 系列分页查询 请求
 */
@Setter
@Getter
@ToString
public class SeriesPageQueryRequest extends PageRequest {

    // 状态（可空，空则查全部）
    private SeriesStateEnum state;

    // 名称关键字（可空）
    private String keyword;

    // 创建者ID（可空，空则查全部；Artist 使用时传入自己的 userId）
    private String creatorId;
}
