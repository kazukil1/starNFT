package cn.kaziki.nft.turbo.admin.controller;

import cn.kaziki.nft.turbo.api.synthesis.model.SynthesisRecipeVO;
import cn.kaziki.nft.turbo.api.synthesis.service.SynthesisFacadeService;
import cn.kaziki.nft.turbo.base.response.PageResponse;
import cn.kaziki.nft.turbo.base.response.SingleResponse;
import cn.kaziki.nft.turbo.web.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 合成配方后台管理（只读查看 + 全局启停风控）
@Slf4j
@RestController
@RequestMapping("admin/synthesis")
@CrossOrigin(origins = "*")
public class AdminSynthesisController {

    @DubboReference(version = "1.0.0")
    private SynthesisFacadeService synthesisFacadeService;

    // 全局启停任意配方（风控用）
    @PostMapping("/recipe/toggle")
    public Result<Boolean> toggleRecipe(@RequestParam Long id, @RequestParam String state) {
        SingleResponse<Boolean> response = synthesisFacadeService.toggleRecipe(id, state);
        return new Result<>(response);
    }

    // 查看全部配方（运营监控）
    @GetMapping("/recipe/list")
    public Result<PageResponse<SynthesisRecipeVO>> listRecipe(@RequestParam(required = false) Long seriesId,
                                                                @RequestParam(defaultValue = "1") int currentPage,
                                                                @RequestParam(defaultValue = "20") int pageSize) {
        PageResponse<SynthesisRecipeVO> page = synthesisFacadeService.pageQueryRecipe(seriesId, currentPage, pageSize);
        return Result.success(page);
    }
}
