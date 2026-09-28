package cn.kaziki.nft.turbo.series.infrastructure.mapper;

import cn.kaziki.nft.turbo.series.entity.Series;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系列 Mapper
 */
@Mapper
public interface SeriesMapper extends BaseMapper<Series> {
}
