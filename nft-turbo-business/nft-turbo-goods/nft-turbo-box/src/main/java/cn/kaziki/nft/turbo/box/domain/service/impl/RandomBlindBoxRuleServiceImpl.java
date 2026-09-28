package cn.kaziki.nft.turbo.box.domain.service.impl;

import cn.kaziki.nft.turbo.api.box.constant.BlindBoxItemStateEnum;
import cn.kaziki.nft.turbo.box.domain.entity.BlindBoxItem;
import cn.kaziki.nft.turbo.box.domain.request.BlindBoxBindMatchRequest;
import cn.kaziki.nft.turbo.box.domain.service.BlindBoxItemService;
import cn.kaziki.nft.turbo.box.domain.service.BlindBoxRuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 盲盒随机分配规则：按 quantity 占比加权随机
 * <p>
 * 流程：查询盲盒下所有 INIT 条目 → 按 collectionId/starAmount 分组 →
 * 各组条目数作为权重 → 加权随机选组 → 组内随机取一条
 */
@Service("randomBlindBoxRuleService")
@Slf4j
public class RandomBlindBoxRuleServiceImpl implements BlindBoxRuleService {

    @Autowired
    private BlindBoxItemService blindBoxItemService;

    @Override
    public Long match(BlindBoxBindMatchRequest request) {
        List<BlindBoxItem> items = blindBoxItemService
                .queryListByBoxIdAndState(request.getBlindBoxId(), BlindBoxItemStateEnum.INIT.name());

        if (items == null || items.isEmpty()) {
            return null;
        }

        // 按 collectionId/starAmount 分组，保持插入顺序
        Map<String, List<BlindBoxItem>> groups = new LinkedHashMap<>();
        for (BlindBoxItem item : items) {
            String key = item.getCollectionId() != null
                    ? "C" + item.getCollectionId()
                    : "S" + item.getStarAmount();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
        }

        // 加权随机选组：权重 = 组内条目数 = 该类型的 quantity 占比
        int totalCount = items.size();
        int rand = ThreadLocalRandom.current().nextInt(totalCount);
        int cumulative = 0;
        List<BlindBoxItem> selectedGroup = null;
        for (List<BlindBoxItem> group : groups.values()) {
            cumulative += group.size();
            if (rand < cumulative) {
                selectedGroup = group;
                break;
            }
        }
        // 兜底：选最后一组
        if (selectedGroup == null) {
            selectedGroup = new ArrayList<>(groups.values()).get(groups.size() - 1);
        }

        // 组内随机取一条
        int idx = ThreadLocalRandom.current().nextInt(selectedGroup.size());
        return selectedGroup.get(idx).getId();
    }
}