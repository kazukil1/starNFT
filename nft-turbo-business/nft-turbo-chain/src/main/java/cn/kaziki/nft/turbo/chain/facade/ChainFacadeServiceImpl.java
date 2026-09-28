package cn.kaziki.nft.turbo.chain.facade;

import cn.kaziki.nft.turbo.api.chain.constant.ChainType;
import cn.kaziki.nft.turbo.api.chain.request.ChainProcessRequest;
import cn.kaziki.nft.turbo.api.chain.response.ChainProcessResponse;
import cn.kaziki.nft.turbo.api.chain.response.data.ChainCreateData;
import cn.kaziki.nft.turbo.api.chain.response.data.ChainOperationData;
import cn.kaziki.nft.turbo.api.chain.service.ChainFacadeService;
import cn.kaziki.nft.turbo.chain.domain.service.ChainService;
import cn.kaziki.nft.turbo.chain.domain.service.ChainServiceFactory;
import cn.kaziki.nft.turbo.rpc.facade.Facade;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import static cn.kaziki.nft.turbo.base.constant.ProfileConstant.PROFILE_DEV;

/**
 *
 */
@DubboService(version = "1.0.0")
public class ChainFacadeServiceImpl implements ChainFacadeService {

    @Value("${nft.turbo.chain.type:MOCK}")
    private String chainType;

    @Value("${spring.profiles.active}")
    private String profile;

    @Autowired
    private ChainServiceFactory chainServiceFactory;

    // 为账户创建链地址
    @Override
    @Facade
    public ChainProcessResponse<ChainCreateData> createAddr(ChainProcessRequest request) {
        return getChainService().createAddr(request);
    }

    // 上链
    @Override
    @Facade
    public ChainProcessResponse<ChainOperationData> chain(ChainProcessRequest request) {
        return getChainService().chain(request);
    }

    // 铸造
    @Override
    @Facade
    public ChainProcessResponse<ChainOperationData> mint(ChainProcessRequest request) {
        return getChainService().mint(request);
    }

    // 转让
    @Override
    @Facade
    public ChainProcessResponse<ChainOperationData> transfer(ChainProcessRequest request) {
        return getChainService().transfer(request);
    }

    // 销毁
    @Override
    @Facade
    public ChainProcessResponse<ChainOperationData> destroy(ChainProcessRequest request) {
        return getChainService().destroy(request);
    }

    // 获取链服务
    private ChainService getChainService() {
        // dev环境下是mock
        if (PROFILE_DEV.equals(profile)) {
            return chainServiceFactory.get(ChainType.MOCK);
        }

        // 通过工厂获取具体的链服务（文昌链）
        ChainService chainService = chainServiceFactory.get(ChainType.valueOf(chainType));
        return chainService;
    }
}
