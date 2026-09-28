package cn.kaziki.nft.turbo.admin.controller;

import cn.kaziki.nft.turbo.api.album.model.AlbumMilestoneVO;
import cn.kaziki.nft.turbo.api.album.request.AlbumMilestoneCreateRequest;
import cn.kaziki.nft.turbo.api.album.request.AlbumMilestoneModifyRequest;
import cn.kaziki.nft.turbo.api.album.service.AlbumFacadeService;
import cn.kaziki.nft.turbo.base.response.PageResponse;
import cn.kaziki.nft.turbo.base.response.SingleResponse;
import cn.kaziki.nft.turbo.web.vo.Result;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

/**
 * 图鉴里程碑后台管理
 */
@Slf4j
@RestController
@RequestMapping("admin/album")
@CrossOrigin(origins = "*")
public class AdminAlbumController {

    @DubboReference(version = "1.0.0")
    private AlbumFacadeService albumFacadeService;

    @PostMapping("/milestone/create")
    public Result<AlbumMilestoneVO> createMilestone(@Valid @RequestBody AlbumMilestoneCreateRequest request) {
        SingleResponse<AlbumMilestoneVO> response = albumFacadeService.createMilestone(request);
        return new Result<>(response);
    }

    @PostMapping("/milestone/modify")
    public Result<AlbumMilestoneVO> modifyMilestone(@Valid @RequestBody AlbumMilestoneModifyRequest request) {
        SingleResponse<AlbumMilestoneVO> response = albumFacadeService.modifyMilestone(request);
        return new Result<>(response);
    }

    @PostMapping("/milestone/toggle")
    public Result<Boolean> toggleMilestone(@RequestParam Long id, @RequestParam String state) {
        SingleResponse<Boolean> response = albumFacadeService.toggleMilestone(id, state);
        return new Result<>(response);
    }

    @GetMapping("/milestone/list")
    public Result<PageResponse<AlbumMilestoneVO>> listMilestone(@RequestParam(required = false) Long seriesId,
                                                                  @RequestParam(defaultValue = "1") int currentPage,
                                                                  @RequestParam(defaultValue = "20") int pageSize) {
        PageResponse<AlbumMilestoneVO> page = albumFacadeService.pageQueryMilestone(seriesId, currentPage, pageSize);
        return Result.success(page);
    }
}
