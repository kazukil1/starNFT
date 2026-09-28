package cn.kaziki.nft.turbo.box.infrastructure.mapper;

import cn.kaziki.nft.turbo.box.domain.entity.BlindBoxInventoryStream;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 藏品库存流水信息 Mapper 接口
 */
@Mapper
public interface BlindBoxInventoryStreamMapper extends BaseMapper<BlindBoxInventoryStream> {
    /**
     * 根据标识符查询
     *
     * @param identifier
     * @param streamType
     * @param blindBoxId
     * @return
     */
    BlindBoxInventoryStream selectByIdentifier(String identifier, String streamType, Long blindBoxId);

}
