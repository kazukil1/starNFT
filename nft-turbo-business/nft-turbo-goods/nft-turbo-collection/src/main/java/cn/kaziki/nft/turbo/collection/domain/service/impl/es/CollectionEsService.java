package cn.kaziki.nft.turbo.collection.domain.service.impl.es;

import cn.kaziki.nft.turbo.base.response.PageResponse;
import cn.kaziki.nft.turbo.collection.domain.entity.Collection;
import cn.kaziki.nft.turbo.collection.domain.service.impl.BaseCollectionService;
import cn.kaziki.nft.turbo.collection.infrastructure.mapper.es.CollectionEsMapper;
import com.alibaba.nacos.shaded.com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.dromara.easyes.core.biz.SAPageInfo;
import org.dromara.easyes.core.conditions.select.LambdaEsQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "spring.elasticsearch.enable", havingValue = "true")
public class CollectionEsService extends BaseCollectionService {
    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Autowired
    private CollectionEsMapper collectionEsMapper;

    @Override
    public PageResponse<Collection> pageQueryByState(String name, String state, int currentPage, int pageSize, String creatorId, Long seriesId, String obtainType, String rarity) {
        Criteria criteria = null;
        if (StringUtils.isNotBlank(name)) {
            criteria = new Criteria("name").is(name).and(new Criteria("state").is(state), new Criteria("deleted").is("0"));
        } else if ((StringUtils.isNotBlank(state))) {
            criteria = new Criteria("state").is(state).and(new Criteria("deleted").is("0"));
        } else {
            criteria = new Criteria("deleted").is("0");
        }
        // Artist 角色：只查自己创建的
        if (StringUtils.isNotBlank(creatorId)) {
            criteria = criteria != null
                    ? criteria.and(new Criteria("creator_id").is(creatorId))
                    : new Criteria("creator_id").is(creatorId);
        }
        // 按系列筛选
        if (seriesId != null) {
            criteria = criteria != null
                    ? criteria.and(new Criteria("series_id").is(seriesId))
                    : new Criteria("series_id").is(seriesId);
        }
        // C 端：只展示包含指定获取途径的藏品
        if (StringUtils.isNotBlank(obtainType)) {
            criteria = criteria != null
                    ? criteria.and(new Criteria("obtain_type").contains(obtainType))
                    : new Criteria("obtain_type").contains(obtainType);
        }
        // 稀有度筛选
        if (StringUtils.isNotBlank(rarity)) {
            criteria = criteria != null
                    ? criteria.and(new Criteria("rarity").is(rarity))
                    : new Criteria("rarity").is(rarity);
        }
        PageRequest pageRequest = PageRequest.of(currentPage - 1, pageSize);
        Query query = new CriteriaQuery(criteria).setPageable(pageRequest).addSort(Sort.by(Sort.Order.desc("create_time")));
        SearchHits<Collection> searchHits = elasticsearchOperations.search(query, Collection.class);

        return PageResponse.of(searchHits.getSearchHits().stream().map(SearchHit::getContent).toList(), (int) searchHits.getTotalHits(), pageSize, currentPage);
    }

    public SAPageInfo<Collection> deepPageQueryByState(String name, String state, int pageSize, Long lastId) {
        LambdaEsQueryWrapper<Collection> queryWrapper = new LambdaEsQueryWrapper<>();
        queryWrapper.match(Collection::getName, name)
                .and(wrapper -> wrapper
                        .match(collection -> collection.getState().name(), state)
                        .match(Collection::getDeleted, "0"))
                .orderByAsc("create_time");

        SAPageInfo<Collection> saPageInfo;
        if (lastId == null) {
            saPageInfo = collectionEsMapper.searchAfterPage(queryWrapper, null, pageSize);
        } else {
            saPageInfo = collectionEsMapper.searchAfterPage(queryWrapper, ImmutableList.of(lastId), 10);
        }
        return saPageInfo;
    }
}
