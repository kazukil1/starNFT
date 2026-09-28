package cn.kaziki.nft.turbo.admin.controller;

import cn.kaziki.nft.turbo.admin.param.AdminCollectionApproveParam;
import cn.kaziki.nft.turbo.admin.param.AdminCollectionAssessParam;
import cn.kaziki.nft.turbo.admin.param.AdminCollectionRejectParam;
import cn.kaziki.nft.turbo.api.collection.constant.CollectionRarity;
import cn.kaziki.nft.turbo.api.collection.constant.CollectionStateEnum;
import cn.kaziki.nft.turbo.api.collection.model.AirDropStreamVO;
import cn.kaziki.nft.turbo.api.collection.model.CollectionVO;
import cn.kaziki.nft.turbo.api.collection.request.AirDropPageQueryRequest;
import cn.kaziki.nft.turbo.api.collection.request.CollectionAssessRequest;
import cn.kaziki.nft.turbo.api.collection.request.CollectionPageQueryRequest;
import cn.kaziki.nft.turbo.api.collection.request.CollectionStateChangeRequest;
import cn.kaziki.nft.turbo.api.collection.response.CollectionModifyResponse;
import cn.kaziki.nft.turbo.api.collection.service.CollectionManageFacadeService;
import cn.kaziki.nft.turbo.api.collection.service.CollectionReadFacadeService;
import cn.kaziki.nft.turbo.base.response.PageResponse;
import cn.kaziki.nft.turbo.web.util.MultiResultConvertor;
import cn.kaziki.nft.turbo.web.vo.MultiResult;
import cn.kaziki.nft.turbo.web.vo.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

/**
 * 藏品后台管理 — 审核/评定
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("admin/collection")
@CrossOrigin(origins = "*")
public class CollectionAdminController {

    @DubboReference(version = "1.0.0")
    private CollectionManageFacadeService collectionManageFacadeService;

    @DubboReference(version = "1.0.0")
    private CollectionReadFacadeService collectionReadFacadeService;

    // 审核通过（上链 + PENDING_REVIEW → SUCCEED）
    @PostMapping("/approve")
    public Result<Long> approve(@Valid @RequestBody AdminCollectionApproveParam param) {
        CollectionStateChangeRequest request = new CollectionStateChangeRequest();
        request.setIdentifier("ADMIN_APPROVE_COLLECTION_" + param.getCollectionId() + "_" + System.currentTimeMillis());
        request.setCollectionId(param.getCollectionId());
        request.setTargetState(CollectionStateEnum.SUCCEED);

        CollectionModifyResponse response = collectionManageFacadeService.approveCollection(request);
        if (response.getSuccess()) {
            return Result.success(response.getCollectionId());
        }
        return Result.error(response.getResponseCode(), response.getResponseMessage());
    }

    // 审核驳回（PENDING_REVIEW → INIT）
    @PostMapping("/reject")
    public Result<Long> reject(@Valid @RequestBody AdminCollectionRejectParam param) {
        CollectionStateChangeRequest request = new CollectionStateChangeRequest();
        request.setIdentifier("ADMIN_REJECT_COLLECTION_" + param.getCollectionId() + "_" + System.currentTimeMillis());
        request.setCollectionId(param.getCollectionId());
        request.setTargetState(CollectionStateEnum.INIT);

        CollectionModifyResponse response = collectionManageFacadeService.rejectCollection(request);
        if (response.getSuccess()) {
            return Result.success(response.getCollectionId());
        }
        return Result.error(response.getResponseCode(), response.getResponseMessage());
    }

    // 藏品评定（稀有度/系列/铸造值）
    @PostMapping("/assessCollection")
    public Result<Long> assessCollection(@Valid @RequestBody AdminCollectionAssessParam param) {
        CollectionAssessRequest request = new CollectionAssessRequest();
        request.setIdentifier("ADMIN_ASSESS_" + param.getCollectionId() + "_" + System.currentTimeMillis());
        request.setCollectionId(param.getCollectionId());
        request.setRarity(CollectionRarity.valueOf(param.getRarity()));
        request.setSeriesId(param.getSeriesId());
        request.setForgeValue(param.getForgeValue());
        CollectionModifyResponse response = collectionManageFacadeService.assessCollection(request);
        if (response.getSuccess()) {
            return Result.success(response.getCollectionId());
        }
        return Result.error(response.getResponseCode(), response.getResponseMessage());
    }

    // 藏品列表
    @GetMapping("/collectionList")
    public MultiResult<CollectionVO> collectionList(String state, String keyword, Long seriesId, int pageSize, int currentPage) {
        CollectionPageQueryRequest collectionPageQueryRequest = new CollectionPageQueryRequest();
        collectionPageQueryRequest.setState(state);
        collectionPageQueryRequest.setKeyword(keyword);
        collectionPageQueryRequest.setSeriesId(seriesId);
        collectionPageQueryRequest.setCurrentPage(currentPage);
        collectionPageQueryRequest.setPageSize(pageSize);
        PageResponse<CollectionVO> pageResponse = collectionReadFacadeService.pageQuery(collectionPageQueryRequest);
        return MultiResultConvertor.convert(pageResponse);
    }

    // 空投列表
    @GetMapping("/airDropList")
    public MultiResult<AirDropStreamVO> airDropList(String collectionId, String userId, int pageSize, int currentPage) {
        AirDropPageQueryRequest request = new AirDropPageQueryRequest();
        request.setCollectionId(collectionId);
        request.setUserId(userId);
        request.setPageSize(pageSize);
        request.setCurrentPage(currentPage);
        PageResponse<AirDropStreamVO> pageResponse = collectionReadFacadeService.pageQueryAirDropList(request);
        return MultiResultConvertor.convert(pageResponse);
    }
}
