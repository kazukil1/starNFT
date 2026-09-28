package cn.kaziki.nft.turbo.album.infrastructure.mapper;

import cn.kaziki.nft.turbo.album.domain.entity.UserAlbumProgress;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserAlbumProgressMapper extends BaseMapper<UserAlbumProgress> {

    @Select("SELECT * FROM user_album_progress WHERE user_id = #{userId} AND series_id = #{seriesId}")
    UserAlbumProgress selectByUserAndSeries(String userId, Long seriesId);
}