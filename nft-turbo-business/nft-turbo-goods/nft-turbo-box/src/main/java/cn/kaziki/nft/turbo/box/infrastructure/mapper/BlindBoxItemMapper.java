package cn.kaziki.nft.turbo.box.infrastructure.mapper;

import cn.kaziki.nft.turbo.box.domain.entity.BlindBoxItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 盲盒条目信息 Mapper 接口
 */
@Mapper
public interface BlindBoxItemMapper extends BaseMapper<BlindBoxItem> {
}
