package cn.kaziki.nft.turbo.synthesis.domain.validator;

import cn.kaziki.nft.turbo.api.star.service.StarAccountFacadeService;
import cn.kaziki.nft.turbo.api.synthesis.request.SynthesisSubmitRequest;
import cn.kaziki.nft.turbo.base.response.SingleResponse;
import cn.kaziki.nft.turbo.synthesis.domain.entity.SynthesisRecipe;
import cn.kaziki.nft.turbo.synthesis.exception.SynthesisException;
import cn.kaziki.nft.turbo.synthesis.infrastructure.mapper.SynthesisRecipeMapper;
import cn.hutool.core.lang.Assert;

import static cn.kaziki.nft.turbo.synthesis.exception.SynthesisErrorCode.STAR_BALANCE_NOT_ENOUGH;

/**
 * 星尘余额校验器 — 用户星尘余额 >= 配方消耗
 */
public class StarBalanceValidator extends BaseSynthesisSubmitValidator {

    private final StarAccountFacadeService starAccountFacadeService;
    private final SynthesisRecipeMapper synthesisRecipeMapper;

    public StarBalanceValidator(StarAccountFacadeService starAccountFacadeService,
                                SynthesisRecipeMapper synthesisRecipeMapper) {
        this.starAccountFacadeService = starAccountFacadeService;
        this.synthesisRecipeMapper = synthesisRecipeMapper;
    }

    public StarBalanceValidator() {
        this.starAccountFacadeService = null;
        this.synthesisRecipeMapper = null;
    }

    @Override
    protected void doValidate(SynthesisSubmitRequest request) throws SynthesisException {
        SynthesisRecipe recipe = synthesisRecipeMapper.selectById(request.getRecipeId());

        SingleResponse<Long> balanceResp = starAccountFacadeService.getBalance(request.getUserId());
        Assert.isTrue(balanceResp.getSuccess() && balanceResp.getData() != null
                        && balanceResp.getData() >= recipe.getStarCost(),
                () -> new SynthesisException(STAR_BALANCE_NOT_ENOUGH));
    }
}
