package cn.kaziki.nft.turbo.collection.infrastructure.mapper;

import cn.kaziki.nft.turbo.collection.domain.entity.CollectionStream;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 藏品流水信息
 */

@Mapper
public interface CollectionStreamMapper extends BaseMapper<CollectionStream> {
    // 根据标识符查询
    CollectionStream selectByIdentifier(String identifier, String streamType, Long collectionId);
}
