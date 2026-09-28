package cn.kaziki.nft.turbo.collection.domain.service.impl.db;

import cn.kaziki.nft.turbo.base.response.PageResponse;
import cn.kaziki.nft.turbo.collection.domain.entity.Collection;
import cn.kaziki.nft.turbo.collection.domain.service.impl.BaseCollectionService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "spring.elasticsearch.enable", havingValue = "false", matchIfMissing = true)
public class CollectionDbService extends BaseCollectionService {
    @Override
    public PageResponse<Collection> pageQueryByState(String keyWord, String state, int currentPage, int pageSize, String creatorId, Long seriesId, String obtainType, String rarity) {
        Page<Collection> page = new Page<>(currentPage,pageSize);
        QueryWrapper<Collection> wrapper = new QueryWrapper<>();
        if(StringUtils.isNotBlank(state)){
            wrapper.eq("state",state);
        }
        if(StringUtils.isNotBlank(keyWord)){
            wrapper.like("name",keyWord);
        }
        if(StringUtils.isNotBlank(creatorId)){
            wrapper.eq("creator_id", creatorId);
        }
        if(seriesId != null){
            wrapper.eq("series_id", seriesId);
        }
        if(StringUtils.isNotBlank(obtainType)){
            wrapper.like("obtain_type", obtainType);
        }
        if(StringUtils.isNotBlank(rarity)){
            wrapper.eq("rarity", rarity);
        }
        wrapper.orderByDesc("gmt_create");
        Page<Collection> collectionPage = this.page(page, wrapper);
        return PageResponse.of(collectionPage.getRecords(),(int) collectionPage.getTotal(),pageSize,currentPage);
    }
}
