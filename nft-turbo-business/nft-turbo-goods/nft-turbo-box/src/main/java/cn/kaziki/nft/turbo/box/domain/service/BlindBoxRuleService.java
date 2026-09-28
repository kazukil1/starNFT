package cn.kaziki.nft.turbo.box.domain.service;

import cn.kaziki.nft.turbo.box.domain.request.BlindBoxBindMatchRequest;

/**
 * 盲盒分配服务
 */
public interface BlindBoxRuleService {
    /**
     * 按照规则进行匹配
     * @param request
     * @return 匹配到的盲盒条目id
     */
    Long match(BlindBoxBindMatchRequest request);
}
