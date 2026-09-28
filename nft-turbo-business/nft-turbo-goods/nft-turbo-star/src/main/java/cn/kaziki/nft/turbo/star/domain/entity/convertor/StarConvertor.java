package cn.kaziki.nft.turbo.star.domain.entity.convertor;

import cn.kaziki.nft.turbo.api.star.model.StarAccountVO;
import cn.kaziki.nft.turbo.api.star.model.StarStreamVO;
import cn.kaziki.nft.turbo.api.star.model.StarVO;
import cn.kaziki.nft.turbo.star.domain.entity.StarAccount;
import cn.kaziki.nft.turbo.star.domain.entity.StarAccountStream;
import cn.kaziki.nft.turbo.star.domain.entity.StarDaily;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 星尘 实体 ↔ VO 转换
 */
@Mapper
public interface StarConvertor {

    StarConvertor INSTANCE = Mappers.getMapper(StarConvertor.class);

    // state 不映射（StarState vs GoodsState 类型不同），由 Service 手动 set
    @Mapping(target = "state", ignore = true)
    StarVO mapToVo(StarDaily entity);

    List<StarVO> mapToVo(List<StarDaily> entities);

    // 账户流水 → VO（mapStruct 替代 BeanUtils）
    StarStreamVO toStreamVO(StarAccountStream stream);

    // 星尘账户 → VO
    StarAccountVO toAccountVO(StarAccount entity);
}
