package cn.kaziki.nft.turbo.synthesis.domain.validator;

import cn.kaziki.nft.turbo.api.synthesis.request.SynthesisSubmitRequest;
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
 * RecipeValidator 单元测试 — 配方存在性 + 状态有效 + 材料数量匹配
 */
public class RecipeValidatorTest {

    private RecipeValidator validator;
    private SynthesisRecipeMapper recipeMapper;

    @Before
    public void setUp() {
        recipeMapper = mock(SynthesisRecipeMapper.class);
        validator = new RecipeValidator(recipeMapper);
    }

    // ==================== 正常流程 ====================

    @Test
    public void testDoValidate_ValidRecipe_NoException() {
        SynthesisRecipe recipe = buildRecipe(1L, "ACTIVE", 3);
        when(recipeMapper.selectById(1L)).thenReturn(recipe);

        SynthesisSubmitRequest request = buildRequest(1L, Arrays.asList("101", "102", "103"));
        validator.doValidate(request);
        // 不抛异常即通过
    }

    // ==================== 配方不存在 ====================

    @Test
    public void testDoValidate_RecipeNotFound_ThrowsException() {
        when(recipeMapper.selectById(999L)).thenReturn(null);

        SynthesisSubmitRequest request = buildRequest(999L, Arrays.asList("101", "102", "103"));
        try {
            validator.doValidate(request);
            fail("应抛出 SynthesisException");
        } catch (SynthesisException e) {
            assertEquals(SynthesisErrorCode.RECIPE_NOT_FOUND.getCode(), e.getErrorCode().getCode());
        }
    }

    // ==================== 配方已停用 ====================

    @Test
    public void testDoValidate_RecipeDisabled_ThrowsException() {
        SynthesisRecipe recipe = buildRecipe(1L, "DISABLED", 3);
        when(recipeMapper.selectById(1L)).thenReturn(recipe);

        SynthesisSubmitRequest request = buildRequest(1L, Arrays.asList("101", "102", "103"));
        try {
            validator.doValidate(request);
            fail("应抛出 SynthesisException");
        } catch (SynthesisException e) {
            assertEquals(SynthesisErrorCode.RECIPE_DISABLED.getCode(), e.getErrorCode().getCode());
        }
    }

    // ==================== 材料数量不匹配 ====================

    @Test
    public void testDoValidate_CardCountMismatch_ThrowsException() {
        SynthesisRecipe recipe = buildRecipe(1L, "ACTIVE", 5); // 配方要求5张
        when(recipeMapper.selectById(1L)).thenReturn(recipe);

        SynthesisSubmitRequest request = buildRequest(1L, Arrays.asList("101", "102", "103")); // 只提交3张
        try {
            validator.doValidate(request);
            fail("应抛出 SynthesisException");
        } catch (SynthesisException e) {
            assertEquals(SynthesisErrorCode.CARD_COUNT_MISMATCH.getCode(), e.getErrorCode().getCode());
        }
    }

    @Test
    public void testDoValidate_CardCountMatchBoundary_NoException() {
        SynthesisRecipe recipe = buildRecipe(1L, "ACTIVE", 1); // 配方要求1张
        when(recipeMapper.selectById(1L)).thenReturn(recipe);

        SynthesisSubmitRequest request = buildRequest(1L, Arrays.asList("101")); // 提交1张
        validator.doValidate(request);
        // 不抛异常即通过
    }

    // ==================== 责任链接续 ====================

    @Test
    public void testChain_NextValidatorCalled_OnSuccess() {
        SynthesisRecipe recipe = buildRecipe(1L, "ACTIVE", 2);
        when(recipeMapper.selectById(1L)).thenReturn(recipe);

        SynthesisSubmitRequest request = buildRequest(1L, Arrays.asList("101", "102"));

        // 构造链：RecipeValidator → MockValidator
        SynthesisSubmitValidator mockNext = mock(SynthesisSubmitValidator.class);
        validator.setNext(mockNext);

        validator.validate(request);
        verify(mockNext, times(1)).validate(request);
    }

    @Test
    public void testChain_NextValidatorNotCalled_OnFailure() {
        when(recipeMapper.selectById(1L)).thenReturn(null); // 配方不存在

        SynthesisSubmitRequest request = buildRequest(1L, Arrays.asList("101", "102"));
        SynthesisSubmitValidator mockNext = mock(SynthesisSubmitValidator.class);
        validator.setNext(mockNext);

        try {
            validator.validate(request);
            fail("应抛出异常");
        } catch (SynthesisException ignored) {
        }
        // 链路中断，下层不应被调用
        verify(mockNext, never()).validate(any());
    }

    // ==================== 辅助方法 ====================

    private SynthesisRecipe buildRecipe(Long id, String state, int cardCount) {
        SynthesisRecipe recipe = new SynthesisRecipe();
        recipe.setId(id);
        recipe.setState(state);
        recipe.setCardCount(cardCount);
        recipe.setSourceRarity("RARE");
        recipe.setTargetRarity("EPIC");
        recipe.setStarCost(100L);
        return recipe;
    }

    private SynthesisSubmitRequest buildRequest(Long recipeId, java.util.List<String> materialIds) {
        SynthesisSubmitRequest request = new SynthesisSubmitRequest();
        request.setRecipeId(recipeId);
        request.setMaterialIds(materialIds);
        request.setIdentifier("test-id-" + System.currentTimeMillis());
        request.setUserId("user-001");
        return request;
    }
}
