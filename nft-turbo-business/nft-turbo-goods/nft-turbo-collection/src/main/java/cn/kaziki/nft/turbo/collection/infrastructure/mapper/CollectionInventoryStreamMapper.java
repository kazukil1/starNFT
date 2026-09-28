package cn.kaziki.nft.turbo.collection.infrastructure.mapper;

import cn.kaziki.nft.turbo.collection.domain.entity.CollectionInventoryStream;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 藏品库存流水信息
 */
@Mapper
public interface CollectionInventoryStreamMapper extends BaseMapper<CollectionInventoryStream> {

    // 根据标识符查询
    CollectionInventoryStream selectByIdentifier(String identifier, String streamType, Long collectionId);
}
