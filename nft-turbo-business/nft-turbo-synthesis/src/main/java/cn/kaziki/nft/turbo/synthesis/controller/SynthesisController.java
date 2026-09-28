package cn.kaziki.nft.turbo.synthesis.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.kaziki.nft.turbo.api.synthesis.model.SynthesisRecipeVO;
import cn.kaziki.nft.turbo.api.synthesis.model.SynthesisStreamVO;
import cn.kaziki.nft.turbo.api.synthesis.request.SynthesisSubmitRequest;
import cn.kaziki.nft.turbo.api.synthesis.service.SynthesisFacadeService;
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

import java.util.UUID;

// 合成系统 C 端接口
@Slf4j
@RestController
@RequestMapping("synthesis")
@RequiredArgsConstructor
public class SynthesisController {

    @Autowired
    private SynthesisFacadeService synthesisFacadeService;

    // 提交合成
    @PostMapping("/submit")
    public Result<String> submit(@Valid @RequestBody SynthesisSubmitRequest request) {
        request.setUserId((String) StpUtil.getLoginId());
        request.setIdentifier("SYNTHESIS_" + UUID.randomUUID());
        SingleResponse<String> response = synthesisFacadeService.submit(request);
        return new Result<>(response);
    }

    // 查询合成进度
    @GetMapping("/query")
    public Result<SynthesisStreamVO> query(@RequestParam String synthesisId) {
        SingleResponse<SynthesisStreamVO> response = synthesisFacadeService.query(synthesisId);
        return new Result<>(response);
    }

    // 配方列表（按系列筛选）
    @GetMapping("/recipe")
    public MultiResult<SynthesisRecipeVO> recipe(@RequestParam(required = false) Long seriesId,
                                                   @RequestParam(defaultValue = "1") int currentPage,
                                                   @RequestParam(defaultValue = "20") int pageSize) {
        PageResponse<SynthesisRecipeVO> page = synthesisFacadeService.pageQueryRecipe(seriesId, currentPage, pageSize);
        return MultiResultConvertor.convert(page);
    }

    // 我的合成历史
    @GetMapping("/myStream")
    public MultiResult<SynthesisStreamVO> myStream(@RequestParam(defaultValue = "1") int currentPage,
                                                     @RequestParam(defaultValue = "20") int pageSize) {
        String userId = (String) StpUtil.getLoginId();
        PageResponse<SynthesisStreamVO> page = synthesisFacadeService.pageQueryMyStream(userId, currentPage, pageSize);
        return MultiResultConvertor.convert(page);
    }
}
