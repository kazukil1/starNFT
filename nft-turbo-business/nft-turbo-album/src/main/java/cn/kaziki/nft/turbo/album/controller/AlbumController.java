package cn.kaziki.nft.turbo.album.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.kaziki.nft.turbo.api.album.model.AlbumClaimResultVO;
import cn.kaziki.nft.turbo.api.album.model.SeriesAlbumVO;
import cn.kaziki.nft.turbo.api.album.model.UserAlbumVO;
import cn.kaziki.nft.turbo.api.album.request.AlbumClaimRequest;
import cn.kaziki.nft.turbo.api.album.service.AlbumFacadeService;
import cn.kaziki.nft.turbo.base.response.PageResponse;
import cn.kaziki.nft.turbo.base.response.SingleResponse;
import cn.kaziki.nft.turbo.web.util.MultiResultConvertor;
import cn.kaziki.nft.turbo.web.vo.MultiResult;
import cn.kaziki.nft.turbo.web.vo.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 图鉴 C 端接口
 */
@Slf4j
@RestController
@RequestMapping("album")
@RequiredArgsConstructor
public class AlbumController {

    @Autowired
    private AlbumFacadeService albumFacadeService;

    /** 全系列图鉴进度列表 */
    @GetMapping("/progress")
    public MultiResult<UserAlbumVO> progress() {
        String userId = (String) StpUtil.getLoginId();
        PageResponse<UserAlbumVO> page = albumFacadeService.pageQueryProgress(userId);
        return MultiResultConvertor.convert(page);
    }

    /** 单系列图鉴详情 */
    @GetMapping("/series")
    public Result<SeriesAlbumVO> series(@RequestParam Long seriesId) {
        String userId = (String) StpUtil.getLoginId();
        SingleResponse<SeriesAlbumVO> response = albumFacadeService.getSeriesDetail(userId, seriesId);
        return new Result<>(response);
    }

    /** 领取里程碑奖励 */
    @PostMapping("/claim")
    public Result<AlbumClaimResultVO> claim(@Valid @RequestBody AlbumClaimRequest request) {
        request.setUserId((String) StpUtil.getLoginId());
        SingleResponse<AlbumClaimResultVO> response = albumFacadeService.claimReward(request);
        return new Result<>(response);
    }
}
