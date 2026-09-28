package cn.kaziki.nft.turbo.star.domain.service.impl;

import cn.kaziki.nft.turbo.api.star.request.StarChangeRequest;
import cn.kaziki.nft.turbo.api.star.request.StarStreamPageQueryRequest;
import cn.kaziki.nft.turbo.base.response.PageResponse;
import cn.kaziki.nft.turbo.star.domain.entity.StarAccount;
import cn.kaziki.nft.turbo.star.domain.entity.StarAccountStream;
import cn.kaziki.nft.turbo.star.domain.service.StarAccountService;
import cn.kaziki.nft.turbo.star.exception.StarException;
import cn.kaziki.nft.turbo.star.infrastructure.mapper.StarAccountMapper;
import cn.kaziki.nft.turbo.star.infrastructure.mapper.StarAccountStreamMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.dao.DuplicateKeyException;

import static cn.kaziki.nft.turbo.star.exception.StarErrorCode.*;

/**
 * 星尘账户领域服务实现
 */
@Slf4j
@Service
public class StarAccountServiceImpl
        extends ServiceImpl<StarAccountMapper, StarAccount>
        implements StarAccountService {

    @Autowired
    private StarAccountStreamMapper starAccountStreamMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StarAccount initAccount(String userId) {
        StarAccount exist = getAccount(userId);
        if (exist != null) {
            return exist;
        }
        StarAccount account = StarAccount.init(userId);
        try {
            boolean result = save(account);
            Assert.isTrue(result, () -> new StarException(ACCOUNT_SAVE_FAILED));
            // 首次创建账户写 INIT 流水
            writeInitStream(account);
            return account;
        } catch (DuplicateKeyException e) {
            // 并发场景下另一个线程已创建，重新查询返回
            log.info("账户已存在（并发创建），userId={}", userId);
            return getAccount(userId);
        }
    }

    @Override
    public StarAccount getAccount(String userId) {
        QueryWrapper<StarAccount> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        return getOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StarAccount increase(StarChangeRequest request) {
        // 幂等校验
        if (isDuplicate(request.getIdentifier())) {
            log.warn("星尘操作已执行，identifier={}", request.getIdentifier());
            return getAccount(request.getUserId());
        }

        StarAccount account = getAccount(request.getUserId());
        if (account == null) {
            throw new StarException(ACCOUNT_NOT_EXIST);
        }
        long amount = request.getAmount();
        account.setBalance(account.getBalance() + amount);
        account.setTotalEarned(account.getTotalEarned() + amount);
        boolean result = updateById(account);
        Assert.isTrue(result, () -> new StarException(ACCOUNT_SAVE_FAILED));

        writeStream(request, account.getBalance(), account);
        return account;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StarAccount decrease(StarChangeRequest request) {
        // 幂等校验
        if (isDuplicate(request.getIdentifier())) {
            log.warn("星尘操作已执行，identifier={}", request.getIdentifier());
            return getAccount(request.getUserId());
        }

        StarAccount account = getAccount(request.getUserId());
        if (account == null) {
            throw new StarException(ACCOUNT_NOT_EXIST);
        }
        long amount = request.getAmount();
        if (account.getBalance() < amount) {
            throw new StarException(BALANCE_NOT_ENOUGH);
        }
        account.setBalance(account.getBalance() - amount);
        account.setTotalSpent(account.getTotalSpent() + amount);
        boolean result = updateById(account);
        Assert.isTrue(result, () -> new StarException(ACCOUNT_SAVE_FAILED));

        writeStream(request, account.getBalance(), account);
        return account;
    }

    @Override
    public PageResponse<StarAccountStream> pageQueryStream(StarStreamPageQueryRequest request) {
        QueryWrapper<StarAccountStream> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", request.getUserId());

        // 日期筛选（startDate / endDate 均为 yyyy-MM-dd 格式）h
        if (StrUtil.isNotBlank(request.getStartDate())) {
            wrapper.ge("gmt_create", request.getStartDate() + " 00:00:00");
        }
        if (StrUtil.isNotBlank(request.getEndDate())) {
            wrapper.le("gmt_create", request.getEndDate() + " 23:59:59");
        }

        wrapper.orderByDesc("gmt_create");

        Page<StarAccountStream> page = new Page<>(request.getCurrentPage(), request.getPageSize());
        Page<StarAccountStream> result = starAccountStreamMapper.selectPage(page, wrapper);
        return PageResponse.of(result.getRecords(), (int) result.getTotal(),
                request.getPageSize(), request.getCurrentPage());
    }

    // 幂等校验：查流水表是否有相同 identifier
    private boolean isDuplicate(String identifier) {
        QueryWrapper<StarAccountStream> wrapper = new QueryWrapper<>();
        wrapper.eq("identifier", identifier);
        return starAccountStreamMapper.selectCount(wrapper) > 0;
    }

    // 写账户初始化流水
    private void writeInitStream(StarAccount account) {
        StarAccountStream stream = StarAccountStream.ofInit(account);
        boolean result = starAccountStreamMapper.insert(stream) > 0;
        Assert.isTrue(result, () -> new StarException(STREAM_SAVE_FAILED));
    }

    // 写流水
    private void writeStream(StarChangeRequest request, Long balanceAfter, StarAccount account) {
        StarAccountStream stream = StarAccountStream.of(account, request, balanceAfter);
        boolean result = starAccountStreamMapper.insert(stream) > 0;
        Assert.isTrue(result, () -> new StarException(STREAM_SAVE_FAILED));
    }
}
