package cn.kaziki.nft.turbo.series.facade;

import cn.kaziki.nft.turbo.api.collection.model.SeriesVO;
import cn.kaziki.nft.turbo.api.collection.request.SeriesPageQueryRequest;
import cn.kaziki.nft.turbo.api.collection.service.CollectionReadFacadeService;
import cn.kaziki.nft.turbo.api.collection.service.SeriesReadFacadeService;
import cn.kaziki.nft.turbo.base.response.PageResponse;
import cn.kaziki.nft.turbo.base.response.SingleResponse;
import cn.kaziki.nft.turbo.rpc.facade.Facade;
import cn.kaziki.nft.turbo.series.entity.Series;
import cn.kaziki.nft.turbo.series.entity.convertor.SeriesConvertor;
import cn.kaziki.nft.turbo.series.exception.SeriesErrorCode;
import cn.kaziki.nft.turbo.series.service.SeriesService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

/**
 * 系列读 rpc服务 实现类
 */
@DubboService(version = "1.0.0")
public class SeriesReadFacadeServiceImpl implements SeriesReadFacadeService {

    @Autowired
    private SeriesService seriesService;
    @Autowired
    private CollectionReadFacadeService collectionReadFacadeService;

    @Override
    @Facade
    public SingleResponse<SeriesVO> queryById(Long seriesId) {
        Series series = seriesService.queryById(seriesId);
        if (series == null) {
            return SingleResponse.fail(SeriesErrorCode.SERIES_NOT_EXIST.getCode(),
                    SeriesErrorCode.SERIES_NOT_EXIST.getMessage());
        }
        SeriesVO vo = SeriesConvertor.mapToVo(series);
        // 通过 Dubbo 获取系列下藏品数
        SingleResponse<Long> countResp = collectionReadFacadeService.countCollectionsBySeriesId(seriesId);
        vo.setCollectionCount(countResp.getSuccess() ? countResp.getData() : 0L);
        return SingleResponse.of(vo);
    }

    @Override
    public PageResponse<SeriesVO> pageQuery(SeriesPageQueryRequest request) {
        PageResponse<Series> pageResponse = seriesService.pageQuery(request);
        List<SeriesVO> voList = SeriesConvertor.mapToVo(pageResponse.getDatas());

        if (!voList.isEmpty()) {
            List<Long> seriesIds = voList.stream().map(SeriesVO::getId).toList();
            // 通过 Dubbo 批量获取各系列下藏品数
            SingleResponse<Map<Long, Long>> countResp = collectionReadFacadeService.countCollectionsBySeriesIds(seriesIds);
            if (countResp.getSuccess() && countResp.getData() != null) {
                Map<Long, Long> countMap = countResp.getData();
                voList.forEach(vo -> vo.setCollectionCount(countMap.getOrDefault(vo.getId(), 0L)));
            }
        }

        return PageResponse.of(voList, pageResponse.getTotal(),
                pageResponse.getPageSize(), pageResponse.getCurrentPage());
    }
}
