package cn.kaziki.nft.turbo.collection.domain.entity.convertor;

import cn.kaziki.nft.turbo.api.collection.model.HeldCollectionDTO;
import cn.kaziki.nft.turbo.api.collection.model.HeldCollectionVO;
import cn.kaziki.nft.turbo.collection.domain.entity.HeldCollection;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 *
 */
@Mapper(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface HeldCollectionConvertor {

    HeldCollectionConvertor INSTANCE = Mappers.getMapper(HeldCollectionConvertor.class);

    /**
     * 转换为vo
     *
     * @param request
     * @return
     */
    public HeldCollectionVO mapToVo(HeldCollection request);

    /**
     * 转换为 DTO
     * @param request
     * @return
     */
    public HeldCollectionDTO mapToDto(HeldCollection request);

    /**
     * 转换为vo
     *
     * @param request
     * @return
     */
    public List<HeldCollectionVO> mapToVo(List<HeldCollection> request);

    /**
     * 转换为实体
     *
     * @param request
     * @return
     */
    public HeldCollection mapToEntity(HeldCollectionVO request);

}
