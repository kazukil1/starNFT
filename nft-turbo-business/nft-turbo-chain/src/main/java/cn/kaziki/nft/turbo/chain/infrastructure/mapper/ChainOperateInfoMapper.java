package cn.kaziki.nft.turbo.chain.infrastructure.mapper;

import cn.kaziki.nft.turbo.chain.domain.entity.ChainOperateInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 链操作 Mapper 接口
 */
@Mapper
public interface ChainOperateInfoMapper extends BaseMapper<ChainOperateInfo> {
    /**
     * 扫描所有
     *
     * @return
     */
    List<ChainOperateInfo> scanAll();

    /**
     * 根据 ID 查询出操作状态为PROCESSING的最小ID
     * @param state
     * @return
     */
    public Long queryMinIdByState(String state);

}
