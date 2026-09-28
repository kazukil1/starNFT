package cn.kaziki.nft.turbo.collection.domain.entity.convertor;

import cn.kaziki.nft.turbo.api.collection.constant.CollectionStateEnum;
import cn.kaziki.nft.turbo.api.collection.model.ArtistCollectionVO;
import cn.kaziki.nft.turbo.api.collection.model.CollectionVO;
import cn.kaziki.nft.turbo.api.collection.request.CollectionCreateRequest;
import cn.kaziki.nft.turbo.api.goods.constant.GoodsState;
import cn.kaziki.nft.turbo.collection.domain.entity.Collection;
import cn.kaziki.nft.turbo.collection.domain.entity.CollectionSnapshot;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.factory.Mappers;

import java.util.Date;
import java.util.List;

@Mapper(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface CollectionConvertor {

    CollectionConvertor INSTANCE = Mappers.getMapper(CollectionConvertor.class);

    /**
     * 转换为C端VO
     */
    @Named("mapToVo")
    @Mapping(target = "inventory", source = "request.saleableInventory")
    @Mapping(target = "state", expression = "java(setState(request.getState(), request.getSaleTime(), request.getSaleableInventory()))")
    @Mapping(target = "collectionState", expression = "java(request.getState() != null ? request.getState().name() : null)")
    public CollectionVO mapToVo(Collection request);

    /**
     * 转换为实体
     * @param request
     * @return
     */
    @Mapping(target = "saleableInventory", source = "request.inventory")
    @Mapping(target = "state", ignore = true)
    public Collection mapToEntity(CollectionVO request);

    /**
     * 转换为实体
     *
     * @param request
     * @return
     */
    public Collection mapToEntity(CollectionCreateRequest request);

    /**
     * 创建快照
     *
     * @param request
     * @return
     */
    @Mapping(target = "collectionId", source = "request.id")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "gmtCreate", ignore = true)
    @Mapping(target = "gmtModified", ignore = true)
    public CollectionSnapshot createSnapshot(Collection request);

    /**
     * 转换为C端VO列表
     */
    @IterableMapping(qualifiedByName = "mapToVo")
    public List<CollectionVO> mapToVo(List<Collection> request);

    /**
     * 转换为Artist端VO（不含持有人数，调用方手动set）
     */
    @Named("mapToArtistVo")
    @Mapping(target = "inventory", source = "request.saleableInventory")
    @Mapping(target = "state", expression = "java(setState(request.getState(), request.getSaleTime(), request.getSaleableInventory()))")
    @Mapping(target = "collectionState", expression = "java(request.getState() != null ? request.getState().name() : null)")
    public ArtistCollectionVO mapToArtistVo(Collection request);

    /**
     * 转换为Artist端VO列表
     */
    @IterableMapping(qualifiedByName = "mapToArtistVo")
    public List<ArtistCollectionVO> mapToArtistVo(List<Collection> request);

    /**
     * 设置状态
     */
    public default GoodsState setState(CollectionStateEnum state, Date saleTime, Long saleableInventory) {
        return CollectionVO.getState(state, saleTime, saleableInventory);
    }
}
