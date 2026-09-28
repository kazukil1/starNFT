package cn.kaziki.nft.turbo.star.infrastructure.mapper;

import cn.kaziki.nft.turbo.star.domain.entity.StarAccountStream;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 星尘账户流水 Mapper
 */
@Mapper
public interface StarAccountStreamMapper extends BaseMapper<StarAccountStream> {

    @Select("SELECT * FROM star_account_stream WHERE user_id = #{userId} ORDER BY gmt_create DESC")
    Page<StarAccountStream> pageQueryByUserId(Page<StarAccountStream> page, String userId);
}
