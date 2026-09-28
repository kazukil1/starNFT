package cn.kaziki.nft.turbo.star.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.kaziki.nft.turbo.api.star.model.StarAccountVO;
import cn.kaziki.nft.turbo.api.star.model.StarStreamVO;
import cn.kaziki.nft.turbo.api.star.model.StarVO;
import cn.kaziki.nft.turbo.api.star.request.StarStreamPageQueryRequest;
import cn.kaziki.nft.turbo.api.star.service.StarAccountFacadeService;
import cn.kaziki.nft.turbo.api.star.service.StarReadFacadeService;
import cn.kaziki.nft.turbo.base.response.PageResponse;
import cn.kaziki.nft.turbo.base.response.SingleResponse;
import cn.kaziki.nft.turbo.web.util.MultiResultConvertor;
import cn.kaziki.nft.turbo.web.vo.MultiResult;
import cn.kaziki.nft.turbo.web.vo.Result;
import cn.kaziki.nft.turbo.api.star.request.StarPageQueryRequest;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 星尘 C 端接口
@Slf4j
@RestController
@RequestMapping("star")
public class StarController {

    @Autowired
    private StarAccountFacadeService starAccountFacadeService;
    @Autowired
    private StarReadFacadeService starReadFacadeService;

    // 查询星尘余额
    @GetMapping("/balance")
    public Result<Long> balance() {
        String userId = (String) StpUtil.getLoginId();
        SingleResponse<Long> response = starAccountFacadeService.getBalance(userId);
        return Result.success(response.getData());
    }

    // 查询星尘账户信息（余额 + 累计获得/消耗 + 状态）
    @GetMapping("/accountInfo")
    public Result<StarAccountVO> accountInfo() {
        String userId = (String) StpUtil.getLoginId();
        SingleResponse<StarAccountVO> response = starAccountFacadeService.getAccountInfo(userId);
        return Result.success(response.getData());
    }

    // 星尘流水分页
    @GetMapping("/stream")
    public MultiResult<StarStreamVO> stream(@RequestParam(defaultValue = "1") int currentPage,
                                            @RequestParam(defaultValue = "20") int pageSize,
                                            @RequestParam(required = false) String startDate,
                                            @RequestParam(required = false) String endDate) {
        String userId = (String) StpUtil.getLoginId();
        StarStreamPageQueryRequest request = new StarStreamPageQueryRequest();
        request.setUserId(userId);
        request.setCurrentPage(currentPage);
        request.setPageSize(pageSize);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        PageResponse<StarStreamVO> page = starAccountFacadeService.pageQueryStream(request);
        return MultiResultConvertor.convert(page);
    }

    // 闪购包列表（C 端，仅 ACTIVE）
    @GetMapping("/starList")
    public MultiResult<StarVO> starList(@RequestParam(defaultValue = "1") int currentPage,
                                         @RequestParam(defaultValue = "10") int pageSize) {
        StarPageQueryRequest request = new StarPageQueryRequest();
        request.setCurrentPage(currentPage);
        request.setPageSize(pageSize);
        request.setState("ACTIVE");
        PageResponse<StarVO> pageResponse = starReadFacadeService.pageQuery(request);
        return MultiResultConvertor.convert(pageResponse);
    }

    // 闪购包详情（含实时库存）
    @GetMapping("/starInfo")
    public Result<StarVO> starInfo(@RequestParam @NotNull Long id) {
        SingleResponse<StarVO> response = starReadFacadeService.queryById(id);
        return new Result<>(response);
    }
}
