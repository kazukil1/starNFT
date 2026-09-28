package cn.kaziki.nft.turbo.box.infrastructure.mapper;

import cn.kaziki.nft.turbo.box.domain.entity.BlindBox;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 盲盒信息 Mapper 接口
 */
@Mapper
public interface BlindBoxMapper extends BaseMapper<BlindBox> {

    /**
     * 根据藏品标识查询藏品信息
     *
     * @param identifier
     * @return
     */
    BlindBox selectByIdentifier(String identifier);


    /**
     * 库存扣减
     *
     * @param id
     * @param quantity
     * @return
     */
    int sale(Long id, Long quantity);

    /**
     * 库存扣减-无hint版
     *
     * @param id
     * @param quantity
     * @return
     */
    int saleWithoutHint(Long id, Long quantity);

    /**
     * 库存退回
     *
     * @param id
     * @param quantity
     * @return
     */
    int cancel(Long id, Long quantity);


    /**
     * 冻结库存
     *
     * @param id
     * @param quantity
     * @return
     */
    int freezeInventory(Long id, Long quantity);

    /**
     * 解冻并扣减库存
     *
     * @param id
     * @param quantity
     * @return
     */
    int unfreezeAndSale(Long id, Long quantity);

    /**
     * 解冻库存
     * @param id
     * @param quantity
     * @return
     */
    int unfreezeInventory(Long id, Long quantity);
}
