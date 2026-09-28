package cn.kaziki.nft.turbo.series.entity.convertor;

import cn.kaziki.nft.turbo.api.collection.model.SeriesVO;
import cn.kaziki.nft.turbo.series.entity.Series;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 系列 Entity → VO 转换
 */
public class SeriesConvertor {

    public static SeriesVO mapToVo(Series series) {
        if (series == null) {
            return null;
        }
        SeriesVO vo = new SeriesVO();
        vo.setId(series.getId());
        vo.setName(series.getName());
        vo.setCover(series.getCover());
        vo.setDescription(series.getDescription());
        vo.setState(series.getState());
        vo.setCreatorId(series.getCreatorId());
        return vo;
    }

    public static List<SeriesVO> mapToVo(List<Series> seriesList) {
        if (seriesList == null) {
            return List.of();
        }
        return seriesList.stream().map(SeriesConvertor::mapToVo).collect(Collectors.toList());
    }
}
