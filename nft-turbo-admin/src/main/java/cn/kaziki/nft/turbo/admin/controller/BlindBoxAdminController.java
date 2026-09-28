package cn.kaziki.nft.turbo.admin.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.kaziki.nft.turbo.admin.infrastructure.exception.AdminException;
import cn.kaziki.nft.turbo.api.box.model.BlindBoxVO;
import cn.kaziki.nft.turbo.api.box.request.BlindBoxPageQueryRequest;
import cn.kaziki.nft.turbo.api.box.service.BlindBoxManageFacadeService;
import cn.kaziki.nft.turbo.api.box.service.BlindBoxReadFacadeService;
import cn.kaziki.nft.turbo.base.response.PageResponse;
import cn.kaziki.nft.turbo.file.FileService;
import cn.kaziki.nft.turbo.web.util.MultiResultConvertor;
import cn.kaziki.nft.turbo.web.vo.MultiResult;
import cn.kaziki.nft.turbo.web.vo.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

import static cn.kaziki.nft.turbo.admin.infrastructure.exception.AdminErrorCode.ADMIN_UPLOAD_PICTURE_FAIL;

/**
 * 盲盒审核管理
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("admin/box")
@CrossOrigin(origins = "*")
public class BlindBoxAdminController {

    @DubboReference(version = "1.0.0")
    private BlindBoxManageFacadeService blindBoxManageFacadeService;

    @DubboReference(version = "1.0.0")
    private BlindBoxReadFacadeService blindBoxReadFacadeService;

    @Autowired
    private FileService fileService;

    /** 上传盲盒封面 */
    @PostMapping("/uploadBlindBox")
    public Result<String> uploadBlindBox(@RequestParam("file_data") MultipartFile file) throws Exception {
        if (null == file) {
            throw new AdminException(ADMIN_UPLOAD_PICTURE_FAIL);
        }
        String userId = (String) StpUtil.getLoginId();
        String prefix = "https://nfturbo-yueyu.oss-cn-beijing.aliyuncs.com/";
        InputStream fileStream = file.getInputStream();
        String path = "box/" + userId + "/" + file.getOriginalFilename();
        var res = fileService.upload(path, fileStream);
        if (!res) {
            throw new AdminException(ADMIN_UPLOAD_PICTURE_FAIL);
        }
        return Result.success(prefix + path);
    }

    /** 上传盲盒条目图片 */
    @PostMapping("/uploadCollection")
    public Result<String> uploadCollection(@RequestParam("file_data") MultipartFile file) throws Exception {
        if (null == file) {
            throw new AdminException(ADMIN_UPLOAD_PICTURE_FAIL);
        }
        String userId = (String) StpUtil.getLoginId();
        String prefix = "https://nfturbo-yueyu.oss-cn-beijing.aliyuncs.com/";
        InputStream fileStream = file.getInputStream();
        String path = "collection/" + userId + "/" + file.getOriginalFilename();
        var res = fileService.upload(path, fileStream);
        if (!res) {
            throw new AdminException(ADMIN_UPLOAD_PICTURE_FAIL);
        }
        return Result.success(prefix + path);
    }

    /** 盲盒列表（Admin 看全部） */
    @GetMapping("/blindBoxList")
    public MultiResult<BlindBoxVO> blindBoxList(String state, String keyword, Long seriesId, int pageSize, int currentPage) {
        BlindBoxPageQueryRequest request = new BlindBoxPageQueryRequest();
        request.setState(state);
        request.setKeyword(keyword);
        request.setSeriesId(seriesId);
        request.setCurrentPage(currentPage);
        request.setPageSize(pageSize);
        PageResponse<BlindBoxVO> pageResponse = blindBoxReadFacadeService.pageQueryBlindBox(request);
        return MultiResultConvertor.convert(pageResponse);
    }

    /** 审核通过（上链 → SUCCEED） */
    @PostMapping("/approve")
    public Result<Boolean> approve(@RequestParam Long boxId) {
        var response = blindBoxManageFacadeService.approve(boxId);
        if (response.getSuccess()) {
            return Result.success(true);
        }
        return Result.error(response.getResponseCode(), response.getResponseMessage());
    }

    /** 驳回（PENDING_REVIEW → INIT） */
    @PostMapping("/reject")
    public Result<Boolean> reject(@RequestParam Long boxId) {
        var response = blindBoxManageFacadeService.reject(boxId);
        if (response.getSuccess()) {
            return Result.success(true);
        }
        return Result.error(response.getResponseCode(), response.getResponseMessage());
    }
}
