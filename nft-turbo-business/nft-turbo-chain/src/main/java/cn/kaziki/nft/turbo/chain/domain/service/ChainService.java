package cn.kaziki.nft.turbo.chain.domain.service;

import cn.kaziki.nft.turbo.api.chain.request.ChainProcessRequest;
import cn.kaziki.nft.turbo.api.chain.request.ChainQueryRequest;
import cn.kaziki.nft.turbo.api.chain.response.ChainProcessResponse;
import cn.kaziki.nft.turbo.api.chain.response.data.ChainCreateData;
import cn.kaziki.nft.turbo.api.chain.response.data.ChainOperationData;
import cn.kaziki.nft.turbo.api.chain.response.data.ChainResultData;
import cn.kaziki.nft.turbo.chain.domain.entity.ChainOperateInfo;

/**
 * 交易链服务
 */
public interface ChainService {

    // 创建交易链地址
    ChainProcessResponse<ChainCreateData> createAddr(ChainProcessRequest request);

    // 上链藏品
    ChainProcessResponse<ChainOperationData> chain(ChainProcessRequest request);

    // 铸造藏品
    ChainProcessResponse<ChainOperationData> mint(ChainProcessRequest request);

    // 交易藏品
    ChainProcessResponse<ChainOperationData> transfer(ChainProcessRequest request);

    // 销毁藏品
    ChainProcessResponse<ChainOperationData> destroy(ChainProcessRequest request);

    // 查询上链交易结果
    ChainProcessResponse<ChainResultData> queryChainResult(ChainQueryRequest request);

    // 发消息
    public void sendMsg(ChainOperateInfo chainOperateInfo, ChainResultData chainResultData);

}
