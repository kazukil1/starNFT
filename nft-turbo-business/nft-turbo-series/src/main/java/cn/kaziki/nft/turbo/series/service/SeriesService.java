package cn.kaziki.nft.turbo.series.service;

import cn.kaziki.nft.turbo.api.collection.request.SeriesCreateRequest;
import cn.kaziki.nft.turbo.api.collection.request.SeriesModifyRequest;
import cn.kaziki.nft.turbo.api.collection.request.SeriesPageQueryRequest;
import cn.kaziki.nft.turbo.api.collection.request.SeriesRemoveRequest;
import cn.kaziki.nft.turbo.base.response.PageResponse;
import cn.kaziki.nft.turbo.series.entity.Series;
import cn.kaziki.nft.turbo.series.exception.SeriesErrorCode;
import cn.kaziki.nft.turbo.series.exception.SeriesException;
import cn.kaziki.nft.turbo.series.infrastructure.mapper.SeriesMapper;
import cn.hutool.core.lang.Assert;
import com.alicp.jetcache.anno.CacheInvalidate;
import com.alicp.jetcache.anno.CacheRefresh;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 系列 领域服务
 */
@Service
public class SeriesService extends ServiceImpl<SeriesMapper, Series> {

    // 创建系列
    public Series createSeries(SeriesCreateRequest request) {
        Series series = Series.create(request);
        boolean result = this.save(series);
        Assert.isTrue(result, () -> new SeriesException(SeriesErrorCode.SERIES_SAVE_FAILED));
        return series;
    }

    // 修改系列
    @CacheInvalidate(name = ":series:cache:id:", key = "#request.seriesId")
    public Series modifySeries(SeriesModifyRequest request) {
        Series series = getById(request.getSeriesId());
        if (series == null) {
            throw new SeriesException(SeriesErrorCode.SERIES_NOT_EXIST);
        }
        series.modify(request);
        boolean result = updateById(series);
        Assert.isTrue(result, () -> new SeriesException(SeriesErrorCode.SERIES_SAVE_FAILED));
        return series;
    }

    // 系列详情
    @Cached(name = ":series:cache:id:", expire = 60, localExpire = 10, timeUnit = TimeUnit.MINUTES, cacheType = CacheType.BOTH, key = "#seriesId", cacheNullValue = true)
    @CacheRefresh(refresh = 50, timeUnit = TimeUnit.MINUTES)// 50分钟刷新一次
    public Series queryById(Long seriesId) {
        return getById(seriesId);
    }

    // 下架系列
    @CacheInvalidate(name = ":series:cache:id:", key = "#request.seriesId")
    public Long remove(SeriesRemoveRequest request) {
        Series series = getById(request.getSeriesId());
        if (series == null) {
            throw new SeriesException(SeriesErrorCode.SERIES_NOT_EXIST);
        }
        series.remove();
        updateById(series);
        return series.getId();
    }

    // 系列分页查询
    public PageResponse<Series> pageQuery(SeriesPageQueryRequest request) {
        Page<Series> page = new Page<>(request.getCurrentPage(), request.getPageSize());
        QueryWrapper<Series> wrapper = new QueryWrapper<>();
        if (request.getState() != null) {
            wrapper.eq("state", request.getState());
        }
        if (request.getKeyword() != null && !request.getKeyword().isEmpty()) {
            wrapper.like("name", request.getKeyword());
        }
        if (request.getCreatorId() != null && !request.getCreatorId().isEmpty()) {
            wrapper.eq("creator_id", request.getCreatorId());
        }
        wrapper.orderBy(true, false, "gmt_create");
        Page<Series> seriesPage = page(page, wrapper);
        return PageResponse.of(seriesPage.getRecords(), (int) seriesPage.getTotal(),
                request.getPageSize(), request.getCurrentPage());
    }
}
