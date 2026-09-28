package cn.kaziki.nft.turbo.synthesis.domain.entity.convertor;

import cn.kaziki.nft.turbo.api.synthesis.model.SynthesisRecipeVO;
import cn.kaziki.nft.turbo.api.synthesis.model.SynthesisStreamVO;
import cn.kaziki.nft.turbo.synthesis.domain.entity.SynthesisRecipe;
import cn.kaziki.nft.turbo.synthesis.domain.entity.SynthesisStream;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 合成模块 Entity ↔ VO 转换器
 */
@Mapper(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface SynthesisConvertor {

    SynthesisConvertor INSTANCE = Mappers.getMapper(SynthesisConvertor.class);

    /** 配方实体 → VO */
    SynthesisRecipeVO toRecipeVO(SynthesisRecipe entity);

    /** 配方实体列表 → VO 列表 */
    List<SynthesisRecipeVO> toRecipeVOList(List<SynthesisRecipe> entities);

    /** 合成流水实体 → VO */
    SynthesisStreamVO toStreamVO(SynthesisStream entity);

    /** 合成流水实体列表 → VO 列表 */
    List<SynthesisStreamVO> toStreamVOList(List<SynthesisStream> entities);
}
