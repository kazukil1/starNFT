package cn.kaziki.nft.turbo.admin.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.kaziki.nft.turbo.admin.infrastructure.exception.AdminException;
import cn.kaziki.nft.turbo.admin.param.AdminStarCreateParam;
import cn.kaziki.nft.turbo.admin.param.AdminStarModifyParam;
import cn.kaziki.nft.turbo.api.star.model.StarVO;
import cn.kaziki.nft.turbo.api.star.request.StarCreateRequest;
import cn.kaziki.nft.turbo.api.star.request.StarModifyInventoryRequest;
import cn.kaziki.nft.turbo.api.star.request.StarModifyRequest;
import cn.kaziki.nft.turbo.api.star.request.StarPageQueryRequest;
import cn.kaziki.nft.turbo.api.star.request.StarRemoveRequest;
import cn.kaziki.nft.turbo.api.star.service.StarManageFacadeService;
import cn.kaziki.nft.turbo.api.star.service.StarReadFacadeService;
import cn.kaziki.nft.turbo.base.response.PageResponse;
import cn.kaziki.nft.turbo.base.response.SingleResponse;
import cn.kaziki.nft.turbo.file.FileService;
import cn.kaziki.nft.turbo.web.vo.Result;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.UUID;

import static cn.kaziki.nft.turbo.admin.infrastructure.exception.AdminErrorCode.ADMIN_UPLOAD_PICTURE_FAIL;
import static cn.kaziki.nft.turbo.api.common.constant.CommonConstant.COMMON_TIME_PATTERN;

// 星尘闪购后台管理
@Slf4j
@RestController
@RequestMapping("admin/star")
@CrossOrigin(origins = "*")
public class AdminStarController {

    @DubboReference(version = "1.0.0")
    private StarManageFacadeService starManageFacadeService;

    @DubboReference(version = "1.0.0")
    private StarReadFacadeService starReadFacadeService;

    @Autowired
    private FileService fileService;

    /** 上传星尘包封面 */
    @PostMapping("/uploadStarCover")
    public Result<String> uploadStarCover(@RequestParam("file_data") MultipartFile file) throws Exception {
        if (null == file) {
            throw new AdminException(ADMIN_UPLOAD_PICTURE_FAIL);
        }
        String userId = (String) StpUtil.getLoginId();
        String prefix = "https://nfturbo-yueyu.oss-cn-beijing.aliyuncs.com/";
        InputStream fileStream = file.getInputStream();
        String path = "star/" + userId + "/" + file.getOriginalFilename();
        var res = fileService.upload(path, fileStream);
        if (!res) {
            throw new AdminException(ADMIN_UPLOAD_PICTURE_FAIL);
        }
        return Result.success(prefix + path);
    }

    // 创建星尘闪购包
    @PostMapping("/create")
    public Result<Long> createStar(@Valid @RequestBody AdminStarCreateParam param) throws Exception {
        StarCreateRequest request = new StarCreateRequest();
        request.setIdentifier(UUID.randomUUID().toString());
        request.setName(param.getName());
        request.setCover(param.getCover());
        request.setDetail(param.getDetail());
        request.setPrice(java.math.BigDecimal.valueOf(param.getPrice()));
        request.setQuantity(param.getQuantity());
        request.setStarAmount(param.getStarAmount());
        SimpleDateFormat sdf = new SimpleDateFormat(COMMON_TIME_PATTERN);
        request.setSaleTime(sdf.parse(param.getSaleTime()));

        SingleResponse<Long> response = starManageFacadeService.create(request);
        return new Result<>(response);
    }

    // 修改星尘闪购包基本信息
    @PostMapping("/modify")
    public Result<Long> modify(@Valid @RequestBody AdminStarModifyParam param) throws Exception {
        String userId = (String) StpUtil.getLoginId();

        StarModifyRequest request = new StarModifyRequest();
        request.setId(param.getId());
        request.setIdentifier(UUID.randomUUID().toString());
        request.setUserId(userId);
        request.setName(param.getName());
        request.setCover(param.getCover());
        request.setDetail(param.getDetail());
        if (param.getPrice() != null) {
            request.setPrice(java.math.BigDecimal.valueOf(param.getPrice()));
        }
        request.setStarAmount(param.getStarAmount());
        if (param.getSaleTime() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat(COMMON_TIME_PATTERN);
            request.setSaleTime(sdf.parse(param.getSaleTime()));
        }

        SingleResponse<Long> response = starManageFacadeService.modify(request);
        return new Result<>(response);
    }

    // 修改星尘闪购包库存（正=追加，负=扣减）
    @PostMapping("/modifyInventory")
    public Result<Long> modifyInventory(@Valid @RequestBody StarModifyInventoryRequest request) {
        if (StringUtils.isBlank(request.getIdentifier())) {
            request.setIdentifier(UUID.randomUUID().toString());
        }
        SingleResponse<Long> response = starManageFacadeService.modifyInventory(request);
        return new Result<>(response);
    }

    // 闪购包分页列表（管理端）
    @GetMapping("/starList")
    public Result<PageResponse<StarVO>> starList(@RequestParam(defaultValue = "1") int currentPage,
                                              @RequestParam(defaultValue = "10") int pageSize,
                                              @RequestParam(required = false) String state) {
        StarPageQueryRequest request = new StarPageQueryRequest();
        request.setCurrentPage(currentPage);
        request.setPageSize(pageSize);
        request.setState(state);
        PageResponse<StarVO> pageResponse = starReadFacadeService.pageQuery(request);
        return Result.success(pageResponse);
    }

    // 下架闪购包
    @PostMapping("/remove")
    public Result<Boolean> remove(@Valid @RequestBody StarRemoveRequest request) {
        if (StringUtils.isBlank(request.getIdentifier())) {
            request.setIdentifier(UUID.randomUUID().toString());
        }
        SingleResponse<Boolean> response = starManageFacadeService.remove(request);
        return new Result<>(response);
    }
}
