package cn.kaziki.nft.turbo.api.synthesis.service;

import cn.kaziki.nft.turbo.api.synthesis.model.SynthesisRecipeVO;
import cn.kaziki.nft.turbo.api.synthesis.model.SynthesisStreamVO;
import cn.kaziki.nft.turbo.api.synthesis.request.SynthesisRecipeCreateRequest;
import cn.kaziki.nft.turbo.api.synthesis.request.SynthesisRecipeModifyRequest;
import cn.kaziki.nft.turbo.api.synthesis.request.SynthesisSubmitRequest;
import cn.kaziki.nft.turbo.base.response.PageResponse;
import cn.kaziki.nft.turbo.base.response.SingleResponse;

// 合成系统 Dubbo 接口
public interface SynthesisFacadeService {

    // 提交合成
    SingleResponse<String> submit(SynthesisSubmitRequest request);

    // 查询合成进度
    SingleResponse<SynthesisStreamVO> query(String synthesisId);

    // 配方分页（Admin 查看全部 / C 端查看）
    PageResponse<SynthesisRecipeVO> pageQueryRecipe(Long seriesId, int currentPage, int pageSize);

    // Artist 查询自己的配方
    PageResponse<SynthesisRecipeVO> pageQueryMyRecipes(String creatorId, Long seriesId, int currentPage, int pageSize);

    // 我的合成历史
    PageResponse<SynthesisStreamVO> pageQueryMyStream(String userId, int currentPage, int pageSize);

    // 创建配方（Admin/Artist）
    SingleResponse<SynthesisRecipeVO> createRecipe(SynthesisRecipeCreateRequest request);

    // 修改配方
    SingleResponse<SynthesisRecipeVO> modifyRecipe(SynthesisRecipeModifyRequest request);

    // 启用/停用配方
    SingleResponse<Boolean> toggleRecipe(Long id, String state);
}
