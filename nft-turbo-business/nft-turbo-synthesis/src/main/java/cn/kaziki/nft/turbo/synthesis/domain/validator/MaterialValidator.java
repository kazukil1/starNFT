package cn.kaziki.nft.turbo.synthesis.domain.validator;

import cn.kaziki.nft.turbo.api.collection.model.HeldCollectionVO;
import cn.kaziki.nft.turbo.api.collection.service.CollectionReadFacadeService;
import cn.kaziki.nft.turbo.api.synthesis.request.SynthesisSubmitRequest;
import cn.kaziki.nft.turbo.base.response.SingleResponse;
import cn.kaziki.nft.turbo.synthesis.domain.entity.SynthesisRecipe;
import cn.kaziki.nft.turbo.synthesis.exception.SynthesisException;
import cn.kaziki.nft.turbo.synthesis.infrastructure.mapper.SynthesisRecipeMapper;
import cn.hutool.core.lang.Assert;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static cn.kaziki.nft.turbo.synthesis.exception.SynthesisErrorCode.MATERIAL_NOT_ACTIVED;
import static cn.kaziki.nft.turbo.synthesis.exception.SynthesisErrorCode.MATERIAL_NOT_OWNED;
import static cn.kaziki.nft.turbo.synthesis.exception.SynthesisErrorCode.MATERIAL_RARITY_MISMATCH;
import static cn.kaziki.nft.turbo.synthesis.exception.SynthesisErrorCode.MATERIAL_NOT_IN_RECIPE;

/**
 * 材料校验器 — 材料归属 + 状态 + 稀有度 + 同系列 + CARD_BASED 指定材料范围
 */
public class MaterialValidator extends BaseSynthesisSubmitValidator {

    private final CollectionReadFacadeService collectionReadFacadeService;
    private final SynthesisRecipeMapper synthesisRecipeMapper;

    public MaterialValidator(CollectionReadFacadeService collectionReadFacadeService,
                             SynthesisRecipeMapper synthesisRecipeMapper) {
        this.collectionReadFacadeService = collectionReadFacadeService;
        this.synthesisRecipeMapper = synthesisRecipeMapper;
    }

    public MaterialValidator() {
        this.collectionReadFacadeService = null;
        this.synthesisRecipeMapper = null;
    }

    @Override
    protected void doValidate(SynthesisSubmitRequest request) throws SynthesisException {
        List<Long> materialIds = request.getMaterialIds().stream()
                .map(Long::valueOf).collect(Collectors.toList());

        SynthesisRecipe recipe = synthesisRecipeMapper.selectById(request.getRecipeId());

        // 批量查询材料卡
        SingleResponse<List<HeldCollectionVO>> materialsResp =
                collectionReadFacadeService.batchQueryHeldCollections(materialIds);
        Assert.isTrue(materialsResp.getSuccess() && materialsResp.getData() != null,
                () -> new SynthesisException(MATERIAL_NOT_OWNED));

        List<HeldCollectionVO> materials = materialsResp.getData();

        // CARD_BASED 配方：解析指定材料藏品ID列表
        List<Long> allowedCollectionIds = null;
        if ("CARD_BASED".equals(recipe.getRecipeType()) && recipe.getSourceCollectionIds() != null) {
            allowedCollectionIds = Arrays.stream(recipe.getSourceCollectionIds().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::valueOf)
                    .collect(Collectors.toList());
        }

        for (HeldCollectionVO hc : materials) {
            // 归属校验
            Assert.isTrue(request.getUserId().equals(hc.getUserId()),
                    () -> new SynthesisException(MATERIAL_NOT_OWNED));
            // 状态校验
            Assert.isTrue("ACTIVED".equals(hc.getState()),
                    () -> new SynthesisException(MATERIAL_NOT_ACTIVED));
            // 稀有度校验
            Assert.isTrue(recipe.getSourceRarity().equals(
                    hc.getRarity() != null ? hc.getRarity().name() : null),
                    () -> new SynthesisException(MATERIAL_RARITY_MISMATCH));
            // CARD_BASED 配方：材料卡必须在配方指定的藏品范围内
            if (allowedCollectionIds != null) {
                Assert.isTrue(allowedCollectionIds.contains(hc.getCollectionId()),
                        () -> new SynthesisException(MATERIAL_NOT_IN_RECIPE));
            }
        }
    }
}
