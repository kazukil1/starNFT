package cn.kaziki.nft.turbo.synthesis.infrastructure.mapper;

import cn.kaziki.nft.turbo.synthesis.domain.entity.SynthesisStream;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SynthesisStreamMapper extends BaseMapper<SynthesisStream> {

    @Select("SELECT * FROM synthesis_stream WHERE identifier = #{identifier}")
    SynthesisStream selectByIdentifier(String identifier);

    @Select("SELECT * FROM synthesis_stream WHERE user_id = #{userId} ORDER BY gmt_create DESC")
    java.util.List<SynthesisStream> selectByUserId(String userId);
}
