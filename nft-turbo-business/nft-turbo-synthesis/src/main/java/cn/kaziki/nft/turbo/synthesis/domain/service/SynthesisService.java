package cn.kaziki.nft.turbo.synthesis.domain.service;

import cn.kaziki.nft.turbo.api.synthesis.request.SynthesisRecipeCreateRequest;
import cn.kaziki.nft.turbo.api.synthesis.request.SynthesisRecipeModifyRequest;
import cn.kaziki.nft.turbo.api.synthesis.request.SynthesisSubmitRequest;
import cn.kaziki.nft.turbo.base.response.PageResponse;
import cn.kaziki.nft.turbo.synthesis.domain.entity.SynthesisRecipe;
import cn.kaziki.nft.turbo.synthesis.domain.entity.SynthesisStream;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

// 合成领域服务
public interface SynthesisService extends IService<SynthesisStream> {

    // 提交合成：校验 → 锁定材料 → 写流水 → 返回 synthesisId
    SynthesisStream submit(SynthesisSubmitRequest request, String userId);

    // 异步执行合成链（锁定→burn→扣星尘→mint）
    void executeAsync(SynthesisStream stream);

    // 查询进度
    SynthesisStream queryByIdentifier(String identifier);

    // 分页查询配方（Admin/C 端）
    List<SynthesisRecipe> pageQueryRecipe(Long seriesId);

    // Artist 查询自己的配方
    List<SynthesisRecipe> pageQueryMyRecipes(String creatorId, Long seriesId);

    // 分页查询我的合成历史（领域层返回实体，Facade 层做 VO 转换）
    PageResponse<SynthesisStream> pageQueryMyStream(String userId, int currentPage, int pageSize);

    // 创建配方
    SynthesisRecipe createRecipe(SynthesisRecipeCreateRequest request);

    // 修改配方
    SynthesisRecipe modifyRecipe(SynthesisRecipeModifyRequest request);

    // 启用/停用配方
    Boolean toggleRecipe(Long id, String state);

    // 扫描超时锁定的合成流水（XXL-Job）
    void scanTimeoutLocks();
}
