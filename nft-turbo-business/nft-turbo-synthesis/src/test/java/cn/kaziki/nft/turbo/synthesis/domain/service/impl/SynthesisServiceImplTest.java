package cn.kaziki.nft.turbo.synthesis.domain.service.impl;

import cn.kaziki.nft.turbo.api.chain.service.ChainFacadeService;
import cn.kaziki.nft.turbo.api.collection.constant.CollectionRarity;
import cn.kaziki.nft.turbo.api.collection.model.HeldCollectionVO;
import cn.kaziki.nft.turbo.api.collection.service.CollectionManageFacadeService;
import cn.kaziki.nft.turbo.api.collection.service.CollectionReadFacadeService;
import cn.kaziki.nft.turbo.api.star.service.StarAccountFacadeService;
import cn.kaziki.nft.turbo.api.synthesis.request.SynthesisRecipeCreateRequest;
import cn.kaziki.nft.turbo.api.synthesis.request.SynthesisRecipeModifyRequest;
import cn.kaziki.nft.turbo.api.synthesis.request.SynthesisSubmitRequest;
import cn.kaziki.nft.turbo.base.response.SingleResponse;
import cn.kaziki.nft.turbo.synthesis.domain.constant.SynthesisStateEnum;
import cn.kaziki.nft.turbo.synthesis.domain.entity.SynthesisRecipe;
import cn.kaziki.nft.turbo.synthesis.domain.entity.SynthesisStream;
import cn.kaziki.nft.turbo.synthesis.exception.SynthesisException;
import cn.kaziki.nft.turbo.synthesis.infrastructure.mapper.SynthesisRecipeMapper;
import cn.kaziki.nft.turbo.synthesis.infrastructure.mapper.SynthesisStreamMapper;
import cn.kaziki.turbo.stream.producer.StreamProducer;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SynthesisServiceImpl 单元测试 — submit / executeAsync / 配方CRUD / 查询
 */
public class SynthesisServiceImplTest {

    private SynthesisServiceImpl service;
    private SynthesisStreamMapper streamMapper;
    private SynthesisRecipeMapper recipeMapper;
    private StreamProducer streamProducer;
    private ApplicationEventPublisher eventPublisher;
    private CollectionReadFacadeService collectionReadFacadeService;
    private CollectionManageFacadeService collectionManageFacadeService;
    private StarAccountFacadeService starAccountFacadeService;
    private ChainFacadeService chainFacadeService;

    @Before
    public void setUp() throws Exception {
        streamMapper = mock(SynthesisStreamMapper.class);
        recipeMapper = mock(SynthesisRecipeMapper.class);
        streamProducer = mock(StreamProducer.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        collectionReadFacadeService = mock(CollectionReadFacadeService.class);
        collectionManageFacadeService = mock(CollectionManageFacadeService.class);
        starAccountFacadeService = mock(StarAccountFacadeService.class);
        chainFacadeService = mock(ChainFacadeService.class);

        service = new SynthesisServiceImpl();
        // 注入 baseMapper（父类 ServiceImpl 维护）
        setField(service, "baseMapper", streamMapper);
        setField(service, "synthesisRecipeMapper", recipeMapper);
        setField(service, "streamProducer", streamProducer);
        setField(service, "eventPublisher", eventPublisher);
        setField(service, "collectionReadFacadeService", collectionReadFacadeService);
        setField(service, "collectionManageFacadeService", collectionManageFacadeService);
        setField(service, "starAccountFacadeService", starAccountFacadeService);
        setField(service, "chainFacadeService", chainFacadeService);
    }

    // ==================== submit — 正常流程 ====================

    @Test
    public void testSubmit_Success_ReturnsStream() {
        SynthesisRecipe recipe = buildRecipe(1L, "RARE", "EPIC", 3, 100L);
        when(recipeMapper.selectById(1L)).thenReturn(recipe);
        when(streamMapper.selectByIdentifier(anyString())).thenReturn(null);

        List<HeldCollectionVO> materials = buildMaterials("user-001", CollectionRarity.RARE, 3);
        when(collectionReadFacadeService.batchQueryHeldCollections(anyList()))
                .thenReturn(SingleResponse.of(materials));
        when(collectionManageFacadeService.batchUpdateHeldCollectionState(anyList(), eq("LOCKED"), eq("user-001")))
                .thenReturn(SingleResponse.of(true));
        // save() → baseMapper.insert()
        when(streamMapper.insert(any(SynthesisStream.class))).thenReturn(1);

        SynthesisSubmitRequest request = buildSubmitRequest("user-001", 1L,
                Arrays.asList("101", "102", "103"), "idempotent-001");

        SynthesisStream result = service.submit(request, "user-001");

        assertNotNull(result);
        assertEquals("user-001", result.getUserId());
        assertEquals(SynthesisStateEnum.SUBMITTED.name(), result.getState());
        assertEquals(Long.valueOf(1L), result.getRecipeId());
    }

    // ==================== submit — 幂等 ====================

    @Test
    public void testSubmit_Idempotent_ReturnsExistingStream() {
        SynthesisStream existing = new SynthesisStream();
        existing.setIdentifier("idempotent-001");
        existing.setState(SynthesisStateEnum.SUBMITTED.name());
        when(streamMapper.selectByIdentifier("idempotent-001")).thenReturn(existing);

        SynthesisSubmitRequest request = buildSubmitRequest("user-001", 1L,
                Arrays.asList("101", "102", "103"), "idempotent-001");

        SynthesisStream result = service.submit(request, "user-001");

        assertSame(existing, result);
        // 幂等返回后不查配方、不锁材料
        verify(recipeMapper, never()).selectById(anyLong());
        verify(collectionManageFacadeService, never()).batchUpdateHeldCollectionState(anyList(), anyString(), anyString());
    }

    // ==================== submit — 异常 ====================

    @Test(expected = SynthesisException.class)
    public void testSubmit_MaterialQueryFailed_ThrowsException() {
        SynthesisRecipe recipe = buildRecipe(1L, "RARE", "EPIC", 3, 100L);
        when(recipeMapper.selectById(1L)).thenReturn(recipe);
        when(streamMapper.selectByIdentifier(anyString())).thenReturn(null);
        when(collectionReadFacadeService.batchQueryHeldCollections(anyList()))
                .thenReturn(SingleResponse.fail("ERROR", "查询失败"));

        SynthesisSubmitRequest request = buildSubmitRequest("user-001", 1L,
                Arrays.asList("101", "102", "103"), "idempotent-003");
        service.submit(request, "user-001");
    }

    // ==================== queryByIdentifier ====================

    @Test
    public void testQueryByIdentifier_Found_ReturnsStream() {
        SynthesisStream stream = new SynthesisStream();
        stream.setIdentifier("synth-001");
        when(streamMapper.selectByIdentifier("synth-001")).thenReturn(stream);

        SynthesisStream result = service.queryByIdentifier("synth-001");
        assertNotNull(result);
        assertEquals("synth-001", result.getIdentifier());
    }

    @Test
    public void testQueryByIdentifier_NotFound_ReturnsNull() {
        when(streamMapper.selectByIdentifier("not-exist")).thenReturn(null);
        assertNull(service.queryByIdentifier("not-exist"));
    }

    // ==================== pageQueryRecipe ====================

    @Test
    public void testPageQueryRecipe_NoSeriesFilter_ReturnsAllActive() {
        List<SynthesisRecipe> recipes = Arrays.asList(
                buildRecipe(1L, "RARE", "EPIC", 3, 100L),
                buildRecipe(2L, "EPIC", "LEGENDARY", 3, 350L)
        );
        when(recipeMapper.selectList(any(QueryWrapper.class))).thenReturn(recipes);

        List<SynthesisRecipe> result = service.pageQueryRecipe(null);
        assertEquals(2, result.size());
    }

    @Test
    public void testPageQueryRecipe_WithSeriesFilter_ReturnsFiltered() {
        when(recipeMapper.selectList(any(QueryWrapper.class))).thenReturn(Collections.emptyList());

        List<SynthesisRecipe> result = service.pageQueryRecipe(100L);
        assertTrue(result.isEmpty());
    }

    // ==================== createRecipe ====================

    @Test
    public void testCreateRecipe_Success_ReturnsRecipe() {
        SynthesisRecipeCreateRequest req = new SynthesisRecipeCreateRequest();
        req.setSeriesId(100L);
        req.setName("R→SR·进阶");
        req.setSourceRarity("RARE");
        req.setTargetRarity("EPIC");
        req.setCardCount(3);
        req.setStarCost(100L);
        req.setCreatorId("artist-001");

        when(recipeMapper.insert(any(SynthesisRecipe.class))).thenReturn(1);

        SynthesisRecipe result = service.createRecipe(req);
        assertNotNull(result);
        assertEquals("ACTIVE", result.getState());
        assertEquals("RARITY_BASED", result.getRecipeType());
        assertEquals("artist-001", result.getCreatorId());
    }

    @Test
    public void testCreateRecipe_DefaultTypeIsRarityBased() {
        SynthesisRecipeCreateRequest req = new SynthesisRecipeCreateRequest();
        req.setSeriesId(100L);
        req.setName("Test");
        req.setSourceRarity("RARE");
        req.setTargetRarity("EPIC");
        req.setCardCount(3);
        req.setStarCost(50L);
        req.setCreatorId("artist-001");

        when(recipeMapper.insert(any(SynthesisRecipe.class))).thenReturn(1);

        SynthesisRecipe result = service.createRecipe(req);
        assertEquals("RARITY_BASED", result.getRecipeType());
    }

    // ==================== modifyRecipe ====================

    @Test
    public void testModifyRecipe_Success_ReturnsUpdated() {
        SynthesisRecipe existing = buildRecipe(1L, "RARE", "EPIC", 3, 100L);
        when(recipeMapper.selectById(1L)).thenReturn(existing);
        when(recipeMapper.updateById(any(SynthesisRecipe.class))).thenReturn(1);

        SynthesisRecipeModifyRequest req = new SynthesisRecipeModifyRequest();
        req.setId(1L);
        req.setName("新名称");
        req.setStarCost(200L);

        SynthesisRecipe result = service.modifyRecipe(req);
        assertEquals("新名称", result.getName());
        assertEquals(Long.valueOf(200L), result.getStarCost());
    }

    @Test(expected = SynthesisException.class)
    public void testModifyRecipe_NotFound_ThrowsException() {
        when(recipeMapper.selectById(999L)).thenReturn(null);

        SynthesisRecipeModifyRequest req = new SynthesisRecipeModifyRequest();
        req.setId(999L);
        service.modifyRecipe(req);
    }

    // ==================== toggleRecipe ====================

    @Test
    public void testToggleRecipe_ActivateToDeactivate_ReturnsTrue() {
        SynthesisRecipe recipe = buildRecipe(1L, "RARE", "EPIC", 3, 100L);
        recipe.setState("ACTIVE");
        when(recipeMapper.selectById(1L)).thenReturn(recipe);
        when(recipeMapper.updateById(any(SynthesisRecipe.class))).thenReturn(1);

        Boolean result = service.toggleRecipe(1L, "DISABLED");
        assertTrue(result);
        assertEquals("DISABLED", recipe.getState());
    }

    @Test(expected = SynthesisException.class)
    public void testToggleRecipe_NotFound_ThrowsException() {
        when(recipeMapper.selectById(999L)).thenReturn(null);
        service.toggleRecipe(999L, "DISABLED");
    }

    // ==================== executeAsync — 终态跳过 ====================

    @Test
    public void testExecuteAsync_AlreadyMinted_Skip() {
        SynthesisStream stream = new SynthesisStream();
        stream.setId(1L);
        stream.setIdentifier("synth-done");
        stream.setState(SynthesisStateEnum.MINTED.name());
        // getById() → baseMapper.selectById()
        when(streamMapper.selectById(1L)).thenReturn(stream);

        service.executeAsync(stream);
        // 终态跳过，不调用任何外部服务
        verify(collectionReadFacadeService, never()).batchQueryHeldCollections(anyList());
    }

    @Test
    public void testExecuteAsync_AlreadyFailed_Skip() {
        SynthesisStream stream = new SynthesisStream();
        stream.setId(1L);
        stream.setIdentifier("synth-failed");
        stream.setState(SynthesisStateEnum.FAILED.name());
        when(streamMapper.selectById(1L)).thenReturn(stream);

        service.executeAsync(stream);
        verify(collectionReadFacadeService, never()).batchQueryHeldCollections(anyList());
    }

    // ==================== 辅助方法 ====================

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = findField(target.getClass(), fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new RuntimeException("Field not found: " + fieldName);
    }

    private SynthesisRecipe buildRecipe(Long id, String sourceRarity, String targetRarity,
                                          int cardCount, long starCost) {
        SynthesisRecipe recipe = new SynthesisRecipe();
        recipe.setId(id);
        recipe.setSeriesId(100L);
        recipe.setName(sourceRarity + "→" + targetRarity);
        recipe.setSourceRarity(sourceRarity);
        recipe.setTargetRarity(targetRarity);
        recipe.setCardCount(cardCount);
        recipe.setStarCost(starCost);
        recipe.setState("ACTIVE");
        recipe.setRecipeType("RARITY_BASED");
        recipe.setCreatorId("artist-001");
        return recipe;
    }

    private SynthesisSubmitRequest buildSubmitRequest(String userId, Long recipeId,
                                                        List<String> materialIds, String identifier) {
        SynthesisSubmitRequest request = new SynthesisSubmitRequest();
        request.setUserId(userId);
        request.setRecipeId(recipeId);
        request.setMaterialIds(materialIds);
        request.setIdentifier(identifier);
        return request;
    }

    private List<HeldCollectionVO> buildMaterials(String userId, CollectionRarity rarity, int count) {
        List<HeldCollectionVO> materials = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            HeldCollectionVO vo = new HeldCollectionVO();
            vo.setId(String.valueOf(100 + i));
            vo.setUserId(userId);
            vo.setState("ACTIVED");
            vo.setRarity(rarity);
            vo.setCollectionId((long) (100 + i));
            vo.setForgeValue(rarity.getBaseForgeValue());
            materials.add(vo);
        }
        return materials;
    }
}
