package cn.kaziki.nft.turbo.star.facade;

import cn.kaziki.nft.turbo.api.star.model.StarAccountVO;
import cn.kaziki.nft.turbo.api.star.model.StarStreamVO;
import cn.kaziki.nft.turbo.api.star.request.StarChangeRequest;
import cn.kaziki.nft.turbo.api.star.request.StarStreamPageQueryRequest;
import cn.kaziki.nft.turbo.api.star.service.StarAccountFacadeService;
import cn.kaziki.nft.turbo.base.response.PageResponse;
import cn.kaziki.nft.turbo.base.response.SingleResponse;
import cn.kaziki.nft.turbo.lock.DistributeLock;
import cn.kaziki.nft.turbo.rpc.facade.Facade;
import cn.kaziki.nft.turbo.star.domain.entity.StarAccount;
import cn.kaziki.nft.turbo.star.domain.entity.StarAccountStream;
import cn.kaziki.nft.turbo.star.domain.entity.convertor.StarConvertor;
import cn.kaziki.nft.turbo.star.domain.service.StarAccountService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 星尘账户 RPC 服务实现
 */
@Slf4j
@DubboService(version = "1.0.0")
public class StarAccountFacadeServiceImpl implements StarAccountFacadeService {

    @Autowired
    private StarAccountService starAccountService;

    @Override
    @Facade
    public SingleResponse<Boolean> initAccount(String userId) {
        starAccountService.initAccount(userId);
        return SingleResponse.of(true);
    }

    @Override
    @Facade
    public SingleResponse<Long> getBalance(String userId) {
        StarAccount account = starAccountService.getAccount(userId);
        return SingleResponse.of(account != null ? account.getBalance() : 0L);
    }

    @Override
    @Facade
    public SingleResponse<StarAccountVO> getAccountInfo(String userId) {
        StarAccount account = starAccountService.getAccount(userId);
        if (account == null) {
            return SingleResponse.of(null);
        }
        StarAccountVO vo = StarConvertor.INSTANCE.toAccountVO(account);
        return SingleResponse.of(vo);
    }

    @Override
    @Facade
    @DistributeLock(keyExpression = "#request.identifier", scene = "STAR_INCREASE")
    public SingleResponse<Long> increase(StarChangeRequest request) {
        StarAccount account = starAccountService.increase(request);
        return SingleResponse.of(account.getBalance());
    }

    @Override
    @Facade
    @DistributeLock(keyExpression = "#request.identifier", scene = "STAR_DECREASE")
    public SingleResponse<Long> decrease(StarChangeRequest request) {
        StarAccount account = starAccountService.decrease(request);
        return SingleResponse.of(account.getBalance());
    }

    @Override
    @Facade
    public PageResponse<StarStreamVO> pageQueryStream(StarStreamPageQueryRequest request) {
        PageResponse<StarAccountStream> page = starAccountService.pageQueryStream(request);
        List<StarStreamVO> vos = page.getDatas().stream()
                .map(StarConvertor.INSTANCE::toStreamVO)
                .collect(Collectors.toList());
        return PageResponse.of(vos, page.getTotal(), page.getPageSize(), page.getCurrentPage());
    }
}
