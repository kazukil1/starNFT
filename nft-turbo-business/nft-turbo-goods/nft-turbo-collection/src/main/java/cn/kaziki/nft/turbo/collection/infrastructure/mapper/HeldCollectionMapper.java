package cn.kaziki.nft.turbo.collection.infrastructure.mapper;

import cn.kaziki.nft.turbo.collection.domain.entity.HeldCollection;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 持有藏品服务的mapper接口
 */

@Mapper
public interface HeldCollectionMapper extends BaseMapper<HeldCollection> {
    // 查询出需要重新上链铸造的最小id
    public Long queryMinIdForMint();

    // 统计某藏品当前持有者人数（去重用户，仅生效状态）
    @Select("SELECT COUNT(DISTINCT user_id) FROM held_collection WHERE collection_id = #{collectionId} AND state = 'ACTIVED'")
    Long countHolderByCollectionId(@Param("collectionId") Long collectionId);

    // 统计用户当前持仓总铸造值（仅生效状态；COALESCE 保证空持仓返回 0 而非 NULL）
    @Select("SELECT COALESCE(SUM(forge_value), 0) FROM held_collection WHERE user_id = #{userId} AND state = 'ACTIVED'")
    Long sumForgeValueByUserId(@Param("userId") String userId);
}
