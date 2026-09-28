package cn.kaziki.nft.turbo.series.facade;

import cn.kaziki.nft.turbo.api.collection.model.SeriesVO;
import cn.kaziki.nft.turbo.api.collection.request.SeriesCreateRequest;
import cn.kaziki.nft.turbo.api.collection.request.SeriesModifyRequest;
import cn.kaziki.nft.turbo.api.collection.request.SeriesRemoveRequest;
import cn.kaziki.nft.turbo.api.collection.service.SeriesManageFacadeService;
import cn.kaziki.nft.turbo.base.response.SingleResponse;
import cn.kaziki.nft.turbo.rpc.facade.Facade;
import cn.kaziki.nft.turbo.series.entity.Series;
import cn.kaziki.nft.turbo.series.entity.convertor.SeriesConvertor;
import cn.kaziki.nft.turbo.series.service.SeriesService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 系列管理 rpc服务 实现类
 */
@DubboService(version = "1.0.0")
public class SeriesManageFacadeServiceImpl implements SeriesManageFacadeService {

    @Autowired
    private SeriesService seriesService;

    @Override
    @Facade
    public SingleResponse<Long> createSeries(SeriesCreateRequest request) {
        Series series = seriesService.createSeries(request);
        return SingleResponse.of(series.getId());
    }

    @Override
    @Facade
    public SingleResponse<SeriesVO> modifySeries(SeriesModifyRequest request) {
        Series series = seriesService.modifySeries(request);
        return SingleResponse.of(SeriesConvertor.mapToVo(series));
    }

    @Override
    @Facade
    public SingleResponse<Long> remove(SeriesRemoveRequest request) {
        Long seriesId = seriesService.remove(request);
        return SingleResponse.of(seriesId);
    }
}
