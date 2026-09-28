package cn.kaziki.nft.turbo.star.infrastructure.mapper;

import cn.kaziki.nft.turbo.star.domain.entity.StarInventoryStream;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 星尘库存流水 Mapper
 */
@Mapper
public interface StarInventoryStreamMapper extends BaseMapper<StarInventoryStream> {

    /** 按幂等号 + 流水类型查询（幂等校验用） */
    @Select("SELECT * FROM star_inventory_stream WHERE identifier = #{identifier} AND stream_type = #{streamType} AND star_id = #{starId}")
    StarInventoryStream selectByIdentifier(@Param("identifier") String identifier,
                                            @Param("streamType") String streamType,
                                            @Param("starId") Long starId);
}
