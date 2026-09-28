package cn.kaziki.nft.turbo.synthesis.domain.validator;

import cn.kaziki.nft.turbo.api.collection.constant.CollectionRarity;
import cn.kaziki.nft.turbo.api.collection.model.HeldCollectionVO;
import cn.kaziki.nft.turbo.api.collection.service.CollectionReadFacadeService;
import cn.kaziki.nft.turbo.api.synthesis.request.SynthesisSubmitRequest;
import cn.kaziki.nft.turbo.base.response.SingleResponse;
import cn.kaziki.nft.turbo.synthesis.domain.entity.SynthesisRecipe;
import cn.kaziki.nft.turbo.synthesis.exception.SynthesisErrorCode;
import cn.kaziki.nft.turbo.synthesis.exception.SynthesisException;
import cn.kaziki.nft.turbo.synthesis.infrastructure.mapper.SynthesisRecipeMapper;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * MaterialValidator 单元测试 — 材料归属 + 状态 + 稀有度 + CARD_BASED 范围
 */
public class MaterialValidatorTest {

    private MaterialValidator validator;
    private CollectionReadFacadeService collectionReadFacadeService;
    private SynthesisRecipeMapper recipeMapper;

    @Before
    public void setUp() {
        collectionReadFacadeService = mock(CollectionReadFacadeService.class);
        recipeMapper = mock(SynthesisRecipeMapper.class);
        validator = new MaterialValidator(collectionReadFacadeService, recipeMapper);
    }

    // ==================== 正常流程 — RARITY_BASED ====================

    @Test
    public void testDoValidate_RarityBased_AllValid_NoException() {
        SynthesisRecipe recipe = buildRecipe("RARITY_BASED", "RARE", 3);
        when(recipeMapper.selectById(1L)).thenReturn(recipe);

        List<HeldCollectionVO> materials = Arrays.asList(
                buildHeldCollection("101", "user-001", "ACTIVED", CollectionRarity.RARE, 100L),
                buildHeldCollection("102", "user-001", "ACTIVED", CollectionRarity.RARE, 101L),
                buildHeldCollection("103", "user-001", "ACTIVED", CollectionRarity.RARE, 102L)
        );
        when(collectionReadFacadeService.batchQueryHeldCollections(anyList()))
                .thenReturn(SingleResponse.of(materials));

        SynthesisSubmitRequest request = buildRequest("user-001", Arrays.asList("101", "102", "103"));
        validator.doValidate(request);
        // 不抛异常即通过
    }

    // ==================== 材料归属校验 ====================

    @Test
    public void testDoValidate_MaterialNotOwned_ThrowsException() {
        SynthesisRecipe recipe = buildRecipe("RARITY_BASED", "RARE", 1);
        when(recipeMapper.selectById(1L)).thenReturn(recipe);

        // 材料属于 user-002，不是 user-001
        List<HeldCollectionVO> materials = Collections.singletonList(
                buildHeldCollection("101", "user-002", "ACTIVED", CollectionRarity.RARE, 100L)
        );
        when(collectionReadFacadeService.batchQueryHeldCollections(anyList()))
                .thenReturn(SingleResponse.of(materials));

        SynthesisSubmitRequest request = buildRequest("user-001", Collections.singletonList("101"));
        try {
            validator.doValidate(request);
            fail("应抛出 SynthesisException");
        } catch (SynthesisException e) {
            assertEquals(SynthesisErrorCode.MATERIAL_NOT_OWNED.getCode(), e.getErrorCode().getCode());
        }
    }

    // ==================== 材料状态校验 ====================

    @Test
    public void testDoValidate_MaterialNotActived_ThrowsException() {
        SynthesisRecipe recipe = buildRecipe("RARITY_BASED", "RARE", 1);
        when(recipeMapper.selectById(1L)).thenReturn(recipe);

        // 材料状态为 LOCKED（已被锁定，不可合成）
        List<HeldCollectionVO> materials = Collections.singletonList(
                buildHeldCollection("101", "user-001", "LOCKED", CollectionRarity.RARE, 100L)
        );
        when(collectionReadFacadeService.batchQueryHeldCollections(anyList()))
                .thenReturn(SingleResponse.of(materials));

        SynthesisSubmitRequest request = buildRequest("user-001", Collections.singletonList("101"));
        try {
            validator.doValidate(request);
            fail("应抛出 SynthesisException");
        } catch (SynthesisException e) {
            assertEquals(SynthesisErrorCode.MATERIAL_NOT_ACTIVED.getCode(), e.getErrorCode().getCode());
        }
    }

    @Test
    public void testDoValidate_MaterialDestroyed_ThrowsException() {
        SynthesisRecipe recipe = buildRecipe("RARITY_BASED", "RARE", 1);
        when(recipeMapper.selectById(1L)).thenReturn(recipe);

        List<HeldCollectionVO> materials = Collections.singletonList(
                buildHeldCollection("101", "user-001", "DESTROYED", CollectionRarity.RARE, 100L)
        );
        when(collectionReadFacadeService.batchQueryHeldCollections(anyList()))
                .thenReturn(SingleResponse.of(materials));

        SynthesisSubmitRequest request = buildRequest("user-001", Collections.singletonList("101"));
        try {
            validator.doValidate(request);
            fail("应抛出 SynthesisException");
        } catch (SynthesisException e) {
            assertEquals(SynthesisErrorCode.MATERIAL_NOT_ACTIVED.getCode(), e.getErrorCode().getCode());
        }
    }

    // ==================== 稀有度不匹配 ====================

    @Test
    public void testDoValidate_RarityMismatch_ThrowsException() {
        // 配方要求 RARE，但材料是 EPIC
        SynthesisRecipe recipe = buildRecipe("RARITY_BASED", "RARE", 1);
        when(recipeMapper.selectById(1L)).thenReturn(recipe);

        List<HeldCollectionVO> materials = Collections.singletonList(
                buildHeldCollection("101", "user-001", "ACTIVED", CollectionRarity.EPIC, 100L)
        );
        when(collectionReadFacadeService.batchQueryHeldCollections(anyList()))
                .thenReturn(SingleResponse.of(materials));

        SynthesisSubmitRequest request = buildRequest("user-001", Collections.singletonList("101"));
        try {
            validator.doValidate(request);
            fail("应抛出 SynthesisException");
        } catch (SynthesisException e) {
            assertEquals(SynthesisErrorCode.MATERIAL_RARITY_MISMATCH.getCode(), e.getErrorCode().getCode());
        }
    }

    // ==================== CARD_BASED 配方 — 材料范围校验 ====================

    @Test
    public void testDoValidate_CardBased_InRange_NoException() {
        // 配方指定材料只能是 collectionId=100, 200, 300
        SynthesisRecipe recipe = buildCardBasedRecipe("100,200,300");
        when(recipeMapper.selectById(1L)).thenReturn(recipe);

        List<HeldCollectionVO> materials = Collections.singletonList(
                buildHeldCollection("101", "user-001", "ACTIVED", CollectionRarity.LEGENDARY, 100L)
        );
        when(collectionReadFacadeService.batchQueryHeldCollections(anyList()))
                .thenReturn(SingleResponse.of(materials));

        SynthesisSubmitRequest request = buildRequest("user-001", Collections.singletonList("101"));
        validator.doValidate(request);
        // 不抛异常即通过
    }

    @Test
    public void testDoValidate_CardBased_NotInRange_ThrowsException() {
        // 配方指定材料只能是 collectionId=100, 200, 300
        SynthesisRecipe recipe = buildCardBasedRecipe("100,200,300");
        when(recipeMapper.selectById(1L)).thenReturn(recipe);

        // 材料 collectionId = 999，不在范围内
        List<HeldCollectionVO> materials = Collections.singletonList(
                buildHeldCollection("101", "user-001", "ACTIVED", CollectionRarity.LEGENDARY, 999L)
        );
        when(collectionReadFacadeService.batchQueryHeldCollections(anyList()))
                .thenReturn(SingleResponse.of(materials));

        SynthesisSubmitRequest request = buildRequest("user-001", Collections.singletonList("101"));
        try {
            validator.doValidate(request);
            fail("应抛出 SynthesisException");
        } catch (SynthesisException e) {
            assertEquals(SynthesisErrorCode.MATERIAL_NOT_IN_RECIPE.getCode(), e.getErrorCode().getCode());
        }
    }

    // ==================== RPC 返回失败 ====================

    @Test
    public void testDoValidate_RpcFail_ThrowsException() {
        SynthesisRecipe recipe = buildRecipe("RARITY_BASED", "RARE", 1);
        when(recipeMapper.selectById(1L)).thenReturn(recipe);
        when(collectionReadFacadeService.batchQueryHeldCollections(anyList()))
                .thenReturn(SingleResponse.fail("RPC_ERROR", "远程调用失败"));

        SynthesisSubmitRequest request = buildRequest("user-001", Collections.singletonList("101"));
        try {
            validator.doValidate(request);
            fail("应抛出 SynthesisException");
        } catch (SynthesisException e) {
            assertEquals(SynthesisErrorCode.MATERIAL_NOT_OWNED.getCode(), e.getErrorCode().getCode());
        }
    }

    @Test
    public void testDoValidate_RpcReturnNullData_ThrowsException() {
        SynthesisRecipe recipe = buildRecipe("RARITY_BASED", "RARE", 1);
        when(recipeMapper.selectById(1L)).thenReturn(recipe);
        when(collectionReadFacadeService.batchQueryHeldCollections(anyList()))
                .thenReturn(SingleResponse.of(null)); // data=null

        SynthesisSubmitRequest request = buildRequest("user-001", Collections.singletonList("101"));
        try {
            validator.doValidate(request);
            fail("应抛出 SynthesisException");
        } catch (SynthesisException e) {
            assertEquals(SynthesisErrorCode.MATERIAL_NOT_OWNED.getCode(), e.getErrorCode().getCode());
        }
    }

    // ==================== 辅助方法 ====================

    private SynthesisRecipe buildRecipe(String recipeType, String sourceRarity, int cardCount) {
        SynthesisRecipe recipe = new SynthesisRecipe();
        recipe.setId(1L);
        recipe.setRecipeType(recipeType);
        recipe.setSourceRarity(sourceRarity);
        recipe.setCardCount(cardCount);
        recipe.setState("ACTIVE");
        return recipe;
    }

    private SynthesisRecipe buildCardBasedRecipe(String sourceCollectionIds) {
        SynthesisRecipe recipe = buildRecipe("CARD_BASED", "LEGENDARY", 1);
        recipe.setSourceCollectionIds(sourceCollectionIds);
        recipe.setTargetCollectionId(500L);
        return recipe;
    }

    private HeldCollectionVO buildHeldCollection(String id, String userId, String state,
                                                   CollectionRarity rarity, Long collectionId) {
        HeldCollectionVO vo = new HeldCollectionVO();
        vo.setId(id);
        vo.setUserId(userId);
        vo.setState(state);
        vo.setRarity(rarity);
        vo.setCollectionId(collectionId);
        return vo;
    }

    private SynthesisSubmitRequest buildRequest(String userId, List<String> materialIds) {
        SynthesisSubmitRequest request = new SynthesisSubmitRequest();
        request.setRecipeId(1L);
        request.setUserId(userId);
        request.setMaterialIds(materialIds);
        request.setIdentifier("test-id-" + System.currentTimeMillis());
        return request;
    }
}
