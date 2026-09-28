package cn.kaziki.nft.turbo.api.collection.request;

import cn.kaziki.nft.turbo.api.collection.constant.CollectionStateEnum;
import cn.kaziki.nft.turbo.api.goods.constant.GoodsEvent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 藏品状态变更请求（审核通过/驳回）
 */
@Setter
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class CollectionStateChangeRequest extends BaseCollectionRequest {

    /** 目标状态 */
    @NotNull(message = "目标状态不能为空")
    private CollectionStateEnum targetState;

    @Override
    public GoodsEvent getEventType() {
        return GoodsEvent.CHAIN;
    }
}
