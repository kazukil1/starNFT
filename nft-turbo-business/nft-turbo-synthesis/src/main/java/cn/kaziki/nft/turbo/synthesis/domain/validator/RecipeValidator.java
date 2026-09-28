package cn.kaziki.nft.turbo.synthesis.domain.validator;

import cn.kaziki.nft.turbo.api.synthesis.request.SynthesisSubmitRequest;
import cn.kaziki.nft.turbo.synthesis.domain.entity.SynthesisRecipe;
import cn.kaziki.nft.turbo.synthesis.exception.SynthesisException;
import cn.kaziki.nft.turbo.synthesis.infrastructure.mapper.SynthesisRecipeMapper;
import cn.hutool.core.lang.Assert;

import static cn.kaziki.nft.turbo.synthesis.exception.SynthesisErrorCode.CARD_COUNT_MISMATCH;
import static cn.kaziki.nft.turbo.synthesis.exception.SynthesisErrorCode.RECIPE_DISABLED;
import static cn.kaziki.nft.turbo.synthesis.exception.SynthesisErrorCode.RECIPE_NOT_FOUND;

/**
 * 配方校验器 — 配方存在 + 状态有效 + 材料数量匹配
 */
public class RecipeValidator extends BaseSynthesisSubmitValidator {

    private final SynthesisRecipeMapper synthesisRecipeMapper;

    public RecipeValidator(SynthesisRecipeMapper synthesisRecipeMapper) {
        this.synthesisRecipeMapper = synthesisRecipeMapper;
    }

    public RecipeValidator() {
        this.synthesisRecipeMapper = null;
    }

    @Override
    protected void doValidate(SynthesisSubmitRequest request) throws SynthesisException {
        SynthesisRecipe recipe = synthesisRecipeMapper.selectById(request.getRecipeId());
        Assert.notNull(recipe, () -> new SynthesisException(RECIPE_NOT_FOUND));
        Assert.isTrue("ACTIVE".equals(recipe.getState()),
                () -> new SynthesisException(RECIPE_DISABLED));
        Assert.isTrue(request.getMaterialIds().size() == recipe.getCardCount(),
                () -> new SynthesisException(CARD_COUNT_MISMATCH));
    }
}
