package cn.kaziki.nft.turbo.collection.domain.service;

import cn.kaziki.nft.turbo.api.collection.request.CollectionPageQueryRequest;
import cn.kaziki.nft.turbo.api.collection.request.CollectionInventoryRequest;
import cn.kaziki.nft.turbo.api.collection.response.CollectionInventoryResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface CollectionInventoryService {
    // 初始化藏品库存
    CollectionInventoryResponse init(CollectionInventoryRequest request);

    // 获取藏品库存
    Integer getInventory(CollectionInventoryRequest request);

    // 扣件藏品库存
    CollectionInventoryResponse decrease(CollectionInventoryRequest request);

    // 获取藏品库存扣减日志
    public List<Object> getInventoryDecreaseLogs(CollectionInventoryRequest request);

    // 增加藏品库存
    CollectionInventoryResponse increase(CollectionPageQueryRequest request);

    // 失效藏品库存
    void invaild(CollectionInventoryRequest request);
}
