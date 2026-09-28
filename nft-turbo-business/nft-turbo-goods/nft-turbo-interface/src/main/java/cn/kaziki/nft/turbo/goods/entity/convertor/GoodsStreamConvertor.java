package cn.kaziki.nft.turbo.goods.entity.convertor;

import cn.kaziki.nft.turbo.api.goods.model.GoodsStreamVO;
import cn.kaziki.nft.turbo.box.domain.entity.BlindBoxInventoryStream;
import cn.kaziki.nft.turbo.collection.domain.entity.CollectionInventoryStream;
import cn.kaziki.nft.turbo.star.domain.entity.StarInventoryStream;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.factory.Mappers;

/**
 * @author Hollis
 */
@Mapper(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface GoodsStreamConvertor {

    GoodsStreamConvertor INSTANCE = Mappers.getMapper(GoodsStreamConvertor.class);

    /**
     * 转换实体
     *
     * @param request
     * @return
     */
    @Mapping(target = "goodsId", source = "request.collectionId")
    @Mapping(target = "goodsType", constant = "COLLECTION")
    public GoodsStreamVO mapToVo(CollectionInventoryStream request);

    /**
     * 转换为vo
     *
     * @param request
     * @return
     */
    @Mapping(target = "goodsId", source = "request.blindBoxId")
    @Mapping(target = "goodsType", constant = "BLIND_BOX")
    public GoodsStreamVO mapToVo(BlindBoxInventoryStream request);

    @Mapping(target = "goodsId", source = "request.starId")
    @Mapping(target = "goodsType", constant = "STAR")
    GoodsStreamVO mapToVo(StarInventoryStream request);
}