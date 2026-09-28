package cn.kaziki.nft.turbo.tcc.mapper;

import cn.kaziki.nft.turbo.tcc.entity.TransactionLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 事务日志 mapper
 */
@Mapper
public interface TransactionLogMapper extends BaseMapper<TransactionLog> {

}
