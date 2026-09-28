package cn.kaziki.nft.turbo.box.domain.service.impl;

import cn.kaziki.nft.turbo.api.box.request.BlindBoxItemPageQueryRequest;
import cn.kaziki.nft.turbo.base.response.PageResponse;
import cn.kaziki.nft.turbo.box.domain.entity.BlindBoxItem;
import cn.kaziki.nft.turbo.box.domain.listener.event.BlindBoxOpenEvent;
import cn.kaziki.nft.turbo.box.domain.service.BlindBoxItemService;
import cn.kaziki.nft.turbo.box.exception.BlindBoxException;
import cn.kaziki.nft.turbo.box.infrastructure.mapper.BlindBoxItemMapper;
import cn.kaziki.nft.turbo.collection.domain.service.impl.HeldCollectionService;
import cn.hutool.core.lang.Assert;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

import static cn.kaziki.nft.turbo.box.exception.BlindBoxErrorCode.BLIND_BOX_ITEM_SAVE_FAILED;
import static cn.kaziki.nft.turbo.box.exception.BlindBoxErrorCode.BLIND_BOX_OPEN_FAILED;

/**
 * 盲盒条目服务实现
 */
@Service
public class BlindBoxItemServiceImpl extends ServiceImpl<BlindBoxItemMapper, BlindBoxItem> implements BlindBoxItemService {
    @Autowired
    private HeldCollectionService heldCollectionService;

    @Autowired
    protected ApplicationContext applicationContext;

    @Autowired
    private BlindBoxItemMapper blindBoxItemMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchCreateItem(List<BlindBoxItem> blindBoxItems) {
        //this调用会使saveBatch中的事务失效，需要在本方法外增加事务
        boolean result = this.saveBatch(blindBoxItems);
        Assert.isTrue(result, () -> new BlindBoxException(BLIND_BOX_ITEM_SAVE_FAILED));
        return true;
    }

    @Override
    public BlindBoxItem open(BlindBoxItem blindBoxItem) {
        // 1.更新盲盒条目状态 ASSIGNED -> OPENING
        blindBoxItem.opening();
        boolean result = this.updateById(blindBoxItem);
        Assert.isTrue(result, () -> new BlindBoxException(BLIND_BOX_OPEN_FAILED));
        // 2.通过事件驱动解耦，具体逻辑在 BlindBoxEventListener 中处理
        applicationContext.publishEvent(new BlindBoxOpenEvent(blindBoxItem.getId()));
        return blindBoxItem;
    }

    @Override
    public BlindBoxItem queryById(Long blindBoxItemId) {
        return this.getById(blindBoxItemId);
    }

    @Override
    public List<BlindBoxItem> queryListById(List<Long> itemIds) {
        List<BlindBoxItem> retList = listByIds(itemIds);
        if (CollectionUtils.isEmpty(retList)) {
            return null;
        }
        return retList;
    }

    @Override
    public List<BlindBoxItem> queryListByBoxIdAndState(Long blindBoxId, String state) {
        QueryWrapper<BlindBoxItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("blind_box_id", blindBoxId);
        if (state != null) {
            queryWrapper.eq("state", state);
        }
        queryWrapper.orderByAsc("id");
        List<BlindBoxItem> retList = list(queryWrapper);
        if (CollectionUtils.isEmpty(retList)) {
            return null;
        }
        return retList;
    }

    @Override
    public PageResponse<BlindBoxItem> pageQueryBlindBoxItem(BlindBoxItemPageQueryRequest request) {
        Page<BlindBoxItem> page = new Page<>(request.getCurrentPage(), request.getPageSize());
        QueryWrapper<BlindBoxItem> wrapper = new QueryWrapper<>();
        wrapper.eq("state", request.getState());
        if (StringUtils.isNotBlank(request.getUserId())) {
            wrapper.eq("user_id", request.getUserId());
        }
        if (request.getBoxId() != null) {
            wrapper.eq("blind_box_id", request.getBoxId());
        }
        wrapper.like("name", request.getKeyword());
        wrapper.orderBy(true, true, "gmt_create");
        Page<BlindBoxItem> blindBoxItemPage = this.page(page, wrapper);
        return PageResponse.of(blindBoxItemPage.getRecords(), (int) blindBoxItemPage.getTotal(), request.getPageSize(), request.getCurrentPage());
    }
}
