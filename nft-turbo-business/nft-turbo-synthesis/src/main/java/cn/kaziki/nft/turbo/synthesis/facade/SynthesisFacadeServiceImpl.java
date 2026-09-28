package cn.kaziki.nft.turbo.synthesis.facade;

import cn.kaziki.nft.turbo.api.collection.model.HeldCollectionVO;
import cn.kaziki.nft.turbo.api.collection.model.SeriesVO;
import cn.kaziki.nft.turbo.api.collection.service.CollectionReadFacadeService;
import cn.kaziki.nft.turbo.api.collection.service.SeriesReadFacadeService;
import cn.kaziki.nft.turbo.api.synthesis.model.SynthesisRecipeVO;
import cn.kaziki.nft.turbo.api.synthesis.model.SynthesisStreamVO;
import cn.kaziki.nft.turbo.api.synthesis.request.SynthesisRecipeCreateRequest;
import cn.kaziki.nft.turbo.api.synthesis.request.SynthesisRecipeModifyRequest;
import cn.kaziki.nft.turbo.api.synthesis.request.SynthesisSubmitRequest;
import cn.kaziki.nft.turbo.api.synthesis.service.SynthesisFacadeService;
import cn.kaziki.nft.turbo.base.response.PageResponse;
import cn.kaziki.nft.turbo.base.response.SingleResponse;
import cn.kaziki.nft.turbo.lock.DistributeLock;
import cn.kaziki.nft.turbo.rpc.facade.Facade;
import cn.kaziki.nft.turbo.synthesis.domain.entity.SynthesisRecipe;
import cn.kaziki.nft.turbo.synthesis.domain.entity.SynthesisStream;
import cn.kaziki.nft.turbo.synthesis.domain.entity.convertor.SynthesisConvertor;
import cn.kaziki.nft.turbo.synthesis.domain.listener.event.SynthesisSubmittedEvent;
import cn.kaziki.nft.turbo.synthesis.domain.service.SynthesisService;
import cn.kaziki.nft.turbo.synthesis.domain.validator.SynthesisSubmitValidator;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// 合成系统 Dubbo 门面
@DubboService(version = "1.0.0")
public class SynthesisFacadeServiceImpl implements SynthesisFacadeService {

    @Autowired
    private SynthesisService synthesisService;
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private SynthesisSubmitValidator synthesisValidatorChain;
    @Autowired
    private CollectionReadFacadeService collectionReadFacadeService;
    @Autowired
    private SeriesReadFacadeService seriesReadFacadeService;

    @Override
    @Facade
    @DistributeLock(keyExpression = "#request.identifier", scene = "SYNTHESIS_SUBMIT")
    public SingleResponse<String> submit(SynthesisSubmitRequest request) {
        // 前置校验（只读，责任链：配方→材料→星尘→铸造值）
        synthesisValidatorChain.validate(request);
        // 执行（锁定+流水，写操作）
        SynthesisStream stream = synthesisService.submit(request, request.getUserId());
        // 发事件，由 SynthesisEventListener 异步执行合成链
        eventPublisher.publishEvent(new SynthesisSubmittedEvent(stream.getIdentifier()));
        return SingleResponse.of(stream.getIdentifier());
    }

    @Override
    @Facade
    public SingleResponse<SynthesisStreamVO> query(String synthesisId) {
        SynthesisStream stream = synthesisService.queryByIdentifier(synthesisId);
        if (stream == null) {
            return SingleResponse.fail("SYNTHESIS_NOT_FOUND", "合成流水不存在");
        }
        SynthesisStreamVO vo = toVO(stream);
        // 合成完成后补产物的名称/封面/编号
        if (stream.getProductHeldId() != null) {
            SingleResponse<HeldCollectionVO> productResp =
                    collectionReadFacadeService.queryHeldCollectionById(stream.getProductHeldId());
            if (productResp.getSuccess() && productResp.getData() != null) {
                HeldCollectionVO product = productResp.getData();
                vo.setProductName(product.getName());
                vo.setProductCover(product.getCover());
                vo.setProductSerialNo(product.getSerialNo());
                vo.setProductRarity(product.getRarity() != null ? product.getRarity().name() : null);
            }
        }
        return SingleResponse.of(vo);
    }

    @Override
    public PageResponse<SynthesisRecipeVO> pageQueryRecipe(Long seriesId, int currentPage, int pageSize) {
        List<SynthesisRecipe> list = synthesisService.pageQueryRecipe(seriesId);
        List<SynthesisRecipeVO> vos = SynthesisConvertor.INSTANCE.toRecipeVOList(list);
        fillSeriesNames(list, vos);
        return PageResponse.of(vos, vos.size(), pageSize, currentPage);
    }

    @Override
    public PageResponse<SynthesisStreamVO> pageQueryMyStream(String userId, int currentPage, int pageSize) {
        PageResponse<SynthesisStream> page = synthesisService.pageQueryMyStream(userId, currentPage, pageSize);
        List<SynthesisStreamVO> vos = SynthesisConvertor.INSTANCE.toStreamVOList(page.getDatas());
        return PageResponse.of(vos, page.getTotal(), page.getPageSize(), page.getCurrentPage());
    }

    @Override
    public PageResponse<SynthesisRecipeVO> pageQueryMyRecipes(String creatorId, Long seriesId, int currentPage, int pageSize) {
        List<SynthesisRecipe> list = synthesisService.pageQueryMyRecipes(creatorId, seriesId);
        List<SynthesisRecipeVO> vos = SynthesisConvertor.INSTANCE.toRecipeVOList(list);
        fillSeriesNames(list, vos);
        return PageResponse.of(vos, vos.size(), pageSize, currentPage);
    }

    @Override
    @Facade
    public SingleResponse<SynthesisRecipeVO> createRecipe(SynthesisRecipeCreateRequest request) {
        SynthesisRecipe recipe = synthesisService.createRecipe(request);
        return SingleResponse.of(toRecipeVO(recipe));
    }

    @Override
    @Facade
    public SingleResponse<SynthesisRecipeVO> modifyRecipe(SynthesisRecipeModifyRequest request) {
        SynthesisRecipe recipe = synthesisService.modifyRecipe(request);
        return SingleResponse.of(toRecipeVO(recipe));
    }

    @Override
    @Facade
    public SingleResponse<Boolean> toggleRecipe(Long id, String state) {
        return SingleResponse.of(synthesisService.toggleRecipe(id, state));
    }

    /** 批量填充配方列表的系列名称（通过 Dubbo 批量查询，避免 N+1） */
    private void fillSeriesNames(List<SynthesisRecipe> recipes, List<SynthesisRecipeVO> vos) {
        Map<Long, String> seriesNameMap = recipes.stream()
                .map(SynthesisRecipe::getSeriesId)
                .distinct()
                .collect(Collectors.toMap(
                        Function.identity(),
                        id -> {
                            SingleResponse<SeriesVO> resp = seriesReadFacadeService.queryById(id);
                            return resp.getSuccess() && resp.getData() != null ? resp.getData().getName() : "";
                        }));
        vos.forEach(vo -> vo.setSeriesName(seriesNameMap.getOrDefault(vo.getSeriesId(), "")));
    }

    private SynthesisRecipeVO toRecipeVO(SynthesisRecipe r) {
        return SynthesisConvertor.INSTANCE.toRecipeVO(r);
    }

    private SynthesisStreamVO toVO(SynthesisStream s) {
        return SynthesisConvertor.INSTANCE.toStreamVO(s);
    }
}
