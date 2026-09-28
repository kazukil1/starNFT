package cn.kaziki.nft.turbo.star.infrastructure.mapper;

import cn.kaziki.nft.turbo.star.domain.entity.StarDaily;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

/**
 * 星尘每日闪购 Mapper（参照 CollectionMapper 模式）
 */
@Mapper
public interface StarDailyMapper extends BaseMapper<StarDaily> {

    /** 扣减可售库存 */
    @Update("UPDATE star_daily_flash SET saleable_inventory = saleable_inventory - #{quantity} "
            + "WHERE id = #{id} AND saleable_inventory >= #{quantity}")
    int sale(Long id, Long quantity);

    /** 回滚可售库存 */
    @Update("UPDATE star_daily_flash SET saleable_inventory = saleable_inventory + #{quantity} "
            + "WHERE id = #{id}")
    int cancel(Long id, Long quantity);

    /** 每日重置可售库存为总发行量（XXL-Job 调用） */
    @Update("UPDATE star_daily_flash SET saleable_inventory = quantity WHERE id = #{id}")
    int resetInventory(Long id);
}
