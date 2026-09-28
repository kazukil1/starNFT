package cn.kaziki.nft.turbo.admin.controller;

import cn.kaziki.nft.turbo.admin.infrastructure.exception.AdminException;
import cn.kaziki.nft.turbo.admin.param.AdminSeriesApproveParam;
import cn.kaziki.nft.turbo.admin.param.AdminSeriesRejectParam;
import cn.kaziki.nft.turbo.api.collection.constant.SeriesStateEnum;
import cn.kaziki.nft.turbo.api.collection.model.SeriesVO;
import cn.kaziki.nft.turbo.api.collection.request.SeriesModifyRequest;
import cn.kaziki.nft.turbo.api.collection.request.SeriesPageQueryRequest;
import cn.kaziki.nft.turbo.api.collection.service.SeriesManageFacadeService;
import cn.kaziki.nft.turbo.api.collection.service.SeriesReadFacadeService;
import cn.kaziki.nft.turbo.base.response.PageResponse;
import cn.kaziki.nft.turbo.base.response.SingleResponse;
import cn.kaziki.nft.turbo.file.FileService;
import cn.kaziki.nft.turbo.web.util.MultiResultConvertor;
import cn.kaziki.nft.turbo.web.vo.MultiResult;
import cn.kaziki.nft.turbo.web.vo.Result;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

import static cn.kaziki.nft.turbo.admin.infrastructure.exception.AdminErrorCode.ADMIN_UPLOAD_PICTURE_FAIL;

/**
 * 藏品系列后台管理（审核/全局查看）
 */
@Slf4j
@RestController
@RequestMapping("admin/series")
@CrossOrigin(origins = "*")
public class SeriesAdminController {

    @DubboReference(version = "1.0.0")
    private SeriesManageFacadeService seriesManageFacadeService;

    @DubboReference(version = "1.0.0")
    private SeriesReadFacadeService seriesReadFacadeService;

    @Autowired
    private FileService fileService;

    // 审核通过（PENDING_REVIEW → INIT）
    @PostMapping("/approve")
    public Result<SeriesVO> approve(@Valid @RequestBody AdminSeriesApproveParam param) {
        SeriesModifyRequest request = new SeriesModifyRequest();
        request.setSeriesId(param.getSeriesId());
        request.setState(SeriesStateEnum.SUCCEED);

        SingleResponse<SeriesVO> response = seriesManageFacadeService.modifySeries(request);
        if (response.getSuccess()) {
            return Result.success(response.getData());
        }
        return Result.error(response.getResponseCode(), response.getResponseMessage());
    }

    // 审核驳回（PENDING_REVIEW → INIT）
    @PostMapping("/reject")
    public Result<SeriesVO> reject(@Valid @RequestBody AdminSeriesRejectParam param) {
        SeriesModifyRequest request = new SeriesModifyRequest();
        request.setSeriesId(param.getSeriesId());
        request.setState(SeriesStateEnum.INIT);
        // TODO: 驳回原因后续可存入 series 扩展字段或审核流水表

        SingleResponse<SeriesVO> response = seriesManageFacadeService.modifySeries(request);
        if (response.getSuccess()) {
            return Result.success(response.getData());
        }
        return Result.error(response.getResponseCode(), response.getResponseMessage());
    }

    // 系列分页列表
    @GetMapping("/seriesList")
    public MultiResult<SeriesVO> seriesList(String state, String keyword, int pageSize, int currentPage) {
        SeriesPageQueryRequest request = new SeriesPageQueryRequest();
        request.setPageSize(pageSize);
        request.setCurrentPage(currentPage);
        request.setKeyword(keyword);
        if (StringUtils.isNotBlank(state)) {
            request.setState(SeriesStateEnum.valueOf(state));
        }
        PageResponse<SeriesVO> pageResponse = seriesReadFacadeService.pageQuery(request);
        return MultiResultConvertor.convert(pageResponse);
    }

    // 系列详情
    @GetMapping("/seriesInfo")
    public Result<SeriesVO> seriesInfo(@RequestParam Long seriesId) {
        SingleResponse<SeriesVO> response = seriesReadFacadeService.queryById(seriesId);
        if (response.getSuccess()) {
            return Result.success(response.getData());
        }
        return Result.error(response.getResponseCode(), response.getResponseMessage());
    }

    /** 上传系列封面 */
    @PostMapping("/uploadCover")
    public Result<String> uploadCover(@RequestParam("file_data") MultipartFile file) throws Exception {
        if (null == file) {
            throw new AdminException(ADMIN_UPLOAD_PICTURE_FAIL);
        }
        String prefix = "https://nfturbo-yueyu.oss-cn-beijing.aliyuncs.com/";
        String filename = file.getOriginalFilename();
        InputStream fileStream = file.getInputStream();
        String path = "series/admin/" + filename;
        var res = fileService.upload(path, fileStream);
        if (!res) {
            throw new AdminException(ADMIN_UPLOAD_PICTURE_FAIL);
        }
        return Result.success(prefix + path);
    }
}
