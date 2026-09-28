package cn.kaziki.nft.turbo.synthesis.domain.validator;

import cn.kaziki.nft.turbo.api.star.service.StarAccountFacadeService;
import cn.kaziki.nft.turbo.api.synthesis.request.SynthesisSubmitRequest;
import cn.kaziki.nft.turbo.base.response.SingleResponse;
import cn.kaziki.nft.turbo.synthesis.domain.entity.SynthesisRecipe;
import cn.kaziki.nft.turbo.synthesis.exception.SynthesisErrorCode;
import cn.kaziki.nft.turbo.synthesis.exception.SynthesisException;
import cn.kaziki.nft.turbo.synthesis.infrastructure.mapper.SynthesisRecipeMapper;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * StarBalanceValidator 单元测试 — 星尘余额校验
 */
public class StarBalanceValidatorTest {

    private StarBalanceValidator validator;
    private StarAccountFacadeService starAccountFacadeService;
    private SynthesisRecipeMapper recipeMapper;

    @Before
    public void setUp() {
        starAccountFacadeService = mock(StarAccountFacadeService.class);
        recipeMapper = mock(SynthesisRecipeMapper.class);
        validator = new StarBalanceValidator(starAccountFacadeService, recipeMapper);
    }

    // ==================== 正常流程 ====================

    @Test
    public void testDoValidate_BalanceSufficient_NoException() {
        SynthesisRecipe recipe = buildRecipe(100L); // 消耗100星尘
        when(recipeMapper.selectById(1L)).thenReturn(recipe);
        when(starAccountFacadeService.getBalance("user-001"))
                .thenReturn(SingleResponse.of(200L)); // 余额200

        SynthesisSubmitRequest request = buildRequest("user-001");
        validator.doValidate(request);
        // 不抛异常即通过
    }

    @Test
    public void testDoValidate_BalanceExactlyMatch_NoException() {
        SynthesisRecipe recipe = buildRecipe(100L);
        when(recipeMapper.selectById(1L)).thenReturn(recipe);
        when(starAccountFacadeService.getBalance("user-001"))
                .thenReturn(SingleResponse.of(100L)); // 刚好够

        SynthesisSubmitRequest request = buildRequest("user-001");
        validator.doValidate(request);
        // 不抛异常即通过
    }

    // ==================== 余额不足 ====================

    @Test
    public void testDoValidate_BalanceInsufficient_ThrowsException() {
        SynthesisRecipe recipe = buildRecipe(500L); // 消耗500星尘
        when(recipeMapper.selectById(1L)).thenReturn(recipe);
        when(starAccountFacadeService.getBalance("user-001"))
                .thenReturn(SingleResponse.of(100L)); // 余额只有100

        SynthesisSubmitRequest request = buildRequest("user-001");
        try {
            validator.doValidate(request);
            fail("应抛出 SynthesisException");
        } catch (SynthesisException e) {
            assertEquals(SynthesisErrorCode.STAR_BALANCE_NOT_ENOUGH.getCode(), e.getErrorCode().getCode());
        }
    }

    @Test
    public void testDoValidate_BalanceZero_ThrowsException() {
        SynthesisRecipe recipe = buildRecipe(1L); // 消耗1星尘
        when(recipeMapper.selectById(1L)).thenReturn(recipe);
        when(starAccountFacadeService.getBalance("user-001"))
                .thenReturn(SingleResponse.of(0L)); // 余额为0

        SynthesisSubmitRequest request = buildRequest("user-001");
        try {
            validator.doValidate(request);
            fail("应抛出 SynthesisException");
        } catch (SynthesisException e) {
            assertEquals(SynthesisErrorCode.STAR_BALANCE_NOT_ENOUGH.getCode(), e.getErrorCode().getCode());
        }
    }

    // ==================== RPC 返回异常 ====================

    @Test
    public void testDoValidate_RpcFail_ThrowsException() {
        SynthesisRecipe recipe = buildRecipe(100L);
        when(recipeMapper.selectById(1L)).thenReturn(recipe);
        when(starAccountFacadeService.getBalance("user-001"))
                .thenReturn(SingleResponse.fail("RPC_ERROR", "远程调用失败"));

        SynthesisSubmitRequest request = buildRequest("user-001");
        try {
            validator.doValidate(request);
            fail("应抛出 SynthesisException");
        } catch (SynthesisException e) {
            assertEquals(SynthesisErrorCode.STAR_BALANCE_NOT_ENOUGH.getCode(), e.getErrorCode().getCode());
        }
    }

    // ==================== 辅助方法 ====================

    private SynthesisRecipe buildRecipe(long starCost) {
        SynthesisRecipe recipe = new SynthesisRecipe();
        recipe.setId(1L);
        recipe.setStarCost(starCost);
        recipe.setCardCount(3);
        recipe.setState("ACTIVE");
        return recipe;
    }

    private SynthesisSubmitRequest buildRequest(String userId) {
        SynthesisSubmitRequest request = new SynthesisSubmitRequest();
        request.setRecipeId(1L);
        request.setUserId(userId);
        request.setMaterialIds(Arrays.asList("101", "102", "103"));
        request.setIdentifier("test-id-" + System.currentTimeMillis());
        return request;
    }
}
