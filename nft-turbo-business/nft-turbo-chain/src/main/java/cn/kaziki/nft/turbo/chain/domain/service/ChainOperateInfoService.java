package cn.kaziki.nft.turbo.chain.domain.service;

import cn.kaziki.nft.turbo.api.chain.constant.ChainOperateBizTypeEnum;
import cn.kaziki.nft.turbo.chain.domain.constant.ChainOperateStateEnum;
import cn.kaziki.nft.turbo.chain.domain.entity.ChainOperateInfo;
import cn.kaziki.nft.turbo.chain.infrastructure.mapper.ChainOperateInfoMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;

/**
 * 区块链操作流水服务
 */
@Service
public class ChainOperateInfoService extends ServiceImpl<ChainOperateInfoMapper, ChainOperateInfo> {

    @Autowired
    private ChainOperateInfoMapper chainOperateInfoMapper;

    // 新增流水
    public Long insertInfo(String chainType, String bizId, String bizType, String operateType, String param,String operationId) {
        ChainOperateInfo operateInfo = new ChainOperateInfo();
        operateInfo.setOperateTime(new Date());
        operateInfo.setChainType(chainType);
        operateInfo.setBizId(bizId);
        operateInfo.setBizType(bizType);
        operateInfo.setOperateType(operateType);
        operateInfo.setParam(param);
        // outBizId -> identifier
        operateInfo.setOutBizId(operationId);
        operateInfo.setState(ChainOperateStateEnum.PROCESSING);

        boolean ret = save(operateInfo);
        if (ret) {
            return operateInfo.getId();
        }
        return null;
    }

    // 更新 区块链操作信息 的返回结果和状态
    public boolean updateResult(Long id, ChainOperateStateEnum state, String result) {
        ChainOperateInfo chainOperateInfo = getById(id);
        chainOperateInfo.setResult(result);
        chainOperateInfo.setState(state);
        if(chainOperateInfo.getBizType().equals(ChainOperateBizTypeEnum.USER.name())){
            chainOperateInfo.setState(ChainOperateStateEnum.SUCCEED);
        }
        if(ChainOperateStateEnum.SUCCEED.equals(state)){
            chainOperateInfo.setSucceedTime(new Date());
        }
        return updateById(chainOperateInfo);
    }

    // 根据业务id、业务类型、外部id 查询链操作信息
    public ChainOperateInfo queryByOutBizId(String bizId, String bizType, String outBizId) {
        QueryWrapper<ChainOperateInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("biz_id", bizId);
        queryWrapper.eq("biz_type", bizType);
        queryWrapper.eq("out_biz_id", outBizId);
        List<ChainOperateInfo> retList = list(queryWrapper);
        if (CollectionUtils.isEmpty(retList)) {
            return null;
        }
        return retList.get(0);
    }

    // 分页查询区块链操作流水
    public List<ChainOperateInfo> pageQueryOperateInfoByState(String state, int pageSize,Long minId) {
        QueryWrapper<ChainOperateInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("state", state);
        wrapper.orderBy(true, true, "gmt_create");
        wrapper.ge("id", minId);
        wrapper.last("limit " + pageSize);
        return this.list(wrapper);
    }

    // 根据状态查询id
    public Long queryMinIdByState(String state) {
        return chainOperateInfoMapper.queryMinIdByState(state);
    }

    public void delete(Long id) {
        removeById(id);
    }

}
