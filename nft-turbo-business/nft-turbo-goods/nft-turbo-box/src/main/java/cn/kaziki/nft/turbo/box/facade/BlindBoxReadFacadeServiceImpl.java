package cn.kaziki.nft.turbo.box.facade;

import cn.kaziki.nft.turbo.api.box.model.BlindBoxItemVO;
import cn.kaziki.nft.turbo.api.box.model.BlindBoxVO;
import cn.kaziki.nft.turbo.api.box.model.HeldBlindBoxVO;
import cn.kaziki.nft.turbo.api.box.request.BlindBoxItemPageQueryRequest;
import cn.kaziki.nft.turbo.api.box.request.BlindBoxPageQueryRequest;
import cn.kaziki.nft.turbo.api.box.service.BlindBoxReadFacadeService;
import cn.kaziki.nft.turbo.api.collection.model.SeriesVO;
import cn.kaziki.nft.turbo.api.collection.service.SeriesReadFacadeService;
import cn.kaziki.nft.turbo.api.goods.constant.GoodsType;
import cn.kaziki.nft.turbo.api.inventory.request.InventoryRequest;
import cn.kaziki.nft.turbo.api.inventory.service.InventoryFacadeService;
import cn.kaziki.nft.turbo.base.response.PageResponse;
import cn.kaziki.nft.turbo.base.response.SingleResponse;
import cn.kaziki.nft.turbo.box.domain.entity.BlindBox;
import cn.kaziki.nft.turbo.box.domain.entity.BlindBoxItem;
import cn.kaziki.nft.turbo.box.domain.entity.convertor.BlindBoxConvertor;
import cn.kaziki.nft.turbo.box.domain.entity.convertor.BlindBoxItemConvertor;
import cn.kaziki.nft.turbo.box.domain.service.BlindBoxItemService;
import cn.kaziki.nft.turbo.box.domain.service.BlindBoxService;
import org.apache.dubbo.config.annotation.DubboService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static cn.kaziki.nft.turbo.box.exception.BlindBoxErrorCode.BLIND_BOX_NOT_EXIST;

/**
 * 盲盒服务
 */
@DubboService(version = "1.0.0")
public class BlindBoxReadFacadeServiceImpl implements BlindBoxReadFacadeService {

    private static final Logger logger = LoggerFactory.getLogger(BlindBoxReadFacadeServiceImpl.class);

    @Autowired
    private BlindBoxService blindBoxService;
    @Autowired
    private BlindBoxItemService blindBoxItemService;
    @Autowired
    private InventoryFacadeService inventoryFacadeService;
    @Autowired
    private SeriesReadFacadeService seriesReadFacadeService;

    /**
     * 根据id查询盲盒详情
     * @param blindBoxId
     * @return
     */
    @Override
    public SingleResponse<BlindBoxVO> queryById(Long blindBoxId) {
        //先查数据库，看是否存在
        BlindBox blindBox = blindBoxService.queryById(blindBoxId);
        if (blindBox == null) {
            return SingleResponse.fail(BLIND_BOX_NOT_EXIST.getCode(), BLIND_BOX_NOT_EXIST.getMessage());
        }

        InventoryRequest request = new InventoryRequest();
        request.setGoodsId(blindBoxId.toString());
        request.setGoodsType(GoodsType.BLIND_BOX);
        //从redis中查缓存
        SingleResponse<Integer> response = inventoryFacadeService.queryInventory(request);

        //没查到的情况下，默认用数据库里面的库存做兜底
        Integer inventory = blindBox.getSaleableInventory().intValue();
        if (response.getSuccess()) {
            inventory = response.getData();
        }

        BlindBoxVO blindBoxVO = BlindBoxConvertor.INSTANCE.mapToVo(blindBox);
        blindBoxVO.setInventory(inventory.longValue());
        blindBoxVO.setState(blindBox.getState(), blindBox.getSaleTime(), inventory.longValue());

        // 填充系列名称
        if (blindBox.getSeriesId() != null) {
            SingleResponse<SeriesVO> sr = seriesReadFacadeService.queryById(blindBox.getSeriesId());
            if (sr.getSuccess() && sr.getData() != null) {
                blindBoxVO.setSeriesName(sr.getData().getName());
            }
        }
        //fixme:这里可以在创建盲盒时，就填充概率表
        // 填充概率表（聚合去重：按 collectionId+starAmount 分组，概率 = 组内条目数 / 总条目数）
        java.util.List<BlindBoxItem> allItems = blindBoxItemService.queryListByBoxIdAndState(blindBoxId, null);
        if (allItems != null && !allItems.isEmpty()) {
            java.util.Map<String, BlindBoxItemVO> aggregated = new java.util.LinkedHashMap<>();
            int totalCount = allItems.size();
            for (BlindBoxItem item : allItems) {
                String key = item.getCollectionId() != null ? "C" + item.getCollectionId() : "S" + item.getStarAmount();
                if (!aggregated.containsKey(key)) {
                    BlindBoxItemVO vo = BlindBoxItemConvertor.INSTANCE.mapToVo(item);
                    vo.setQuantity(1L);
                    aggregated.put(key, vo);
                } else {
                    BlindBoxItemVO vo = aggregated.get(key);
                    vo.setQuantity(vo.getQuantity() != null ? vo.getQuantity() + 1 : 2L);
                }
            }
            // 计算各条目概率 = 数量 / 总条目数 × 100
            java.math.BigDecimal hundred = new java.math.BigDecimal("100.00");
            for (BlindBoxItemVO vo : aggregated.values()) {
                vo.setProbability(
                    new java.math.BigDecimal(vo.getQuantity())
                        .multiply(hundred)
                        .divide(new java.math.BigDecimal(totalCount), 2, java.math.RoundingMode.HALF_UP)
                );
            }
            blindBoxVO.setItems(new java.util.ArrayList<>(aggregated.values()));
        }

        return SingleResponse.of(blindBoxVO);
    }

    @Override
    public SingleResponse<BlindBoxItemVO> queryBlindBoxItemById(Long blindBoxItemId) {
        BlindBoxItem blindBoxItem = blindBoxItemService.queryById(blindBoxItemId);
        return SingleResponse.of(BlindBoxItemConvertor.INSTANCE.mapToVo(blindBoxItem));
    }

    @Override
    public PageResponse<BlindBoxVO> pageQueryBlindBox(BlindBoxPageQueryRequest request) {
        PageResponse<BlindBox> blindBoxPage = blindBoxService.pageQueryByState(request.getKeyword(), request.getState(), request.getCreatorId(), request.getSeriesId(), request.getCurrentPage(), request.getPageSize());
        List<BlindBoxVO> voList = BlindBoxConvertor.INSTANCE.mapToVo(blindBoxPage.getDatas());
        // 通过 Dubbo 填充系列名
        for (int i = 0; i < voList.size(); i++) {
            BlindBox bb = blindBoxPage.getDatas().get(i);
            if (bb.getSeriesId() != null) {
                SingleResponse<SeriesVO> sr = seriesReadFacadeService.queryById(bb.getSeriesId());
                if (sr.getSuccess() && sr.getData() != null) {
                    voList.get(i).setSeriesName(sr.getData().getName());
                }
            }
        }
        return PageResponse.of(voList, blindBoxPage.getTotal(), blindBoxPage.getPageSize(), request.getCurrentPage());
    }

    @Override
    public PageResponse<HeldBlindBoxVO> pageQueryBlindBoxItem(BlindBoxItemPageQueryRequest request) {
        PageResponse<BlindBoxItem> blindBoxItemPage = blindBoxItemService.pageQueryBlindBoxItem(request);
        return PageResponse.of(BlindBoxItemConvertor.INSTANCE.mapToHeldVo(blindBoxItemPage.getDatas()), blindBoxItemPage.getTotal(), blindBoxItemPage.getPageSize(), request.getCurrentPage());
    }
}
