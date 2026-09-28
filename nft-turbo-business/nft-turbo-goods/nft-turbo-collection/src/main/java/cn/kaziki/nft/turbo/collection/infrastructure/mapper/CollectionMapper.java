package cn.kaziki.nft.turbo.collection.infrastructure.mapper;

import cn.kaziki.nft.turbo.collection.domain.entity.Collection;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import jakarta.validation.constraints.Min;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CollectionMapper extends BaseMapper<Collection> {
    // 库存扣减
    int sale(Long id, Long quantity);

    // 库存扣减-无hint版
    int saleWithoutHint(Long id, Long quantity);

    // 库存退回
    int cancel(Long id, Long quantity);

    // 冻结库存
    int freezeInventory(Long id, Long quantity);

    // 解冻并扣减库存
    int unfreezeAndSale(Long id, Long quantity);

    //解冻库存
    int unfreezeInventory(Long id, Long quantity);

    int airDrop(Long collectionId, @Min(0L) Long quantity);
}
