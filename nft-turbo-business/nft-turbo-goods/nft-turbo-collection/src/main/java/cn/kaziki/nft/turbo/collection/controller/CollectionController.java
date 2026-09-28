package cn.kaziki.nft.turbo.collection.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.kaziki.nft.turbo.api.chain.constant.ChainOperateBizTypeEnum;
import cn.kaziki.nft.turbo.api.chain.constant.ChainOperateTypeEnum;
import cn.kaziki.nft.turbo.api.chain.request.ChainProcessRequest;
import cn.kaziki.nft.turbo.api.chain.service.ChainFacadeService;
import cn.kaziki.nft.turbo.api.collection.constant.CollectionStateEnum;
import cn.kaziki.nft.turbo.api.collection.model.CollectionVO;
import cn.kaziki.nft.turbo.api.collection.model.ForgeRankVO;
import cn.kaziki.nft.turbo.api.collection.model.HeldCollectionVO;
import cn.kaziki.nft.turbo.api.collection.request.CollectionPageQueryRequest;
import cn.kaziki.nft.turbo.api.collection.request.HeldCollectionPageQueryRequest;
import cn.kaziki.nft.turbo.api.collection.service.CollectionReadFacadeService;
import cn.kaziki.nft.turbo.api.goods.constant.GoodsType;
import cn.kaziki.nft.turbo.api.goods.service.GoodsFacadeService;
import cn.kaziki.nft.turbo.api.user.request.UserQueryRequest;
import cn.kaziki.nft.turbo.api.user.response.UserQueryResponse;
import cn.kaziki.nft.turbo.api.user.response.data.UserInfo;
import cn.kaziki.nft.turbo.api.user.service.UserFacadeService;
import cn.kaziki.nft.turbo.base.response.PageResponse;
import cn.kaziki.nft.turbo.base.response.SingleResponse;
import cn.kaziki.nft.turbo.collection.domain.entity.HeldCollection;
import cn.kaziki.nft.turbo.collection.domain.request.HeldCollectionDestroyRequest;
import cn.kaziki.nft.turbo.collection.domain.request.HeldCollectionTransferRequest;
import cn.kaziki.nft.turbo.collection.domain.service.impl.HeldCollectionService;
import cn.kaziki.nft.turbo.collection.exception.CollectionException;
import cn.kaziki.nft.turbo.collection.param.DestroyParam;
import cn.kaziki.nft.turbo.collection.param.TransferParam;
import cn.kaziki.nft.turbo.web.util.MultiResultConvertor;
import cn.kaziki.nft.turbo.web.vo.MultiResult;
import cn.kaziki.nft.turbo.web.vo.Result;
import cn.hutool.core.lang.Assert;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

import static cn.kaziki.nft.turbo.api.auth.constant.AuthErrorCode.USER_NOT_EXIST;
import static cn.kaziki.nft.turbo.api.common.constant.CommonConstant.SEPARATOR;
import static cn.kaziki.nft.turbo.api.order.constant.OrderErrorCode.BUYER_STATUS_ABNORMAL;
import static cn.kaziki.nft.turbo.api.order.constant.OrderErrorCode.TRANSFER_SELF_ERROR;
import static cn.kaziki.nft.turbo.collection.exception.CollectionErrorCode.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("collection")
public class CollectionController {

    @Autowired
    private CollectionReadFacadeService collectionReadFacadeService;
    @Autowired
    private HeldCollectionService heldCollectionService;
    @Autowired
    private UserFacadeService userFacadeService;
    @Autowired
    private ChainFacadeService chainFacadeService;
    @Autowired
    private GoodsFacadeService goodsFacadeService;

    // 分页查询藏品列表
    @GetMapping("/collectionList")
    public MultiResult<CollectionVO> collectionList(@NotBlank String state, String keyword, int pageSize, int currentPage){
        // C端过滤：不允许查询草稿和待审核状态的藏品
        if (CollectionStateEnum.INIT.name().equals(state) || CollectionStateEnum.PENDING_REVIEW.name().equals(state)) {
            return MultiResult.successMulti(Collections.emptyList(), 0L, currentPage, pageSize);
        }
        CollectionPageQueryRequest collectionPageQueryRequest = new CollectionPageQueryRequest();
        collectionPageQueryRequest.setState(state);
        collectionPageQueryRequest.setKeyword(keyword);
        collectionPageQueryRequest.setCurrentPage(currentPage);
        collectionPageQueryRequest.setPageSize(pageSize);
        // C 端默认过滤：仅展示支持直接购买的藏品
        collectionPageQueryRequest.setObtainType("DIRECT_SALE");
        PageResponse<CollectionVO> pageResponse = collectionReadFacadeService.pageQuery(collectionPageQueryRequest);
        return MultiResultConvertor.convert(pageResponse);
    }

    // 查询藏品详情
    @GetMapping("collectionInfo")
    public Result<CollectionVO> collectionInfo(@NotBlank String collectionId){
        // fixme goodsFacadeService能不能换成collection
        CollectionVO collectionVO = (CollectionVO) goodsFacadeService.getGoods(collectionId, GoodsType.COLLECTION);
        if (collectionVO.canBook()) {
            try {
                String userId = (String) StpUtil.getLoginId();
                Boolean hasBooked = goodsFacadeService.isGoodsBooked(collectionId, GoodsType.COLLECTION, userId);
                collectionVO.setHasBooked(hasBooked);
            } catch (Exception e) {
                //如果用户未登录或其他异常
                collectionVO.setHasBooked(false);
            }
        }
        return Result.success(collectionVO);
    }

    // 用户持有藏品列表
    @GetMapping("heldCollectionList")
    public MultiResult<HeldCollectionVO> heldColelctionList(String state, String keyword, int pageSize, int currentPage){
        String userId = (String) StpUtil.getLoginId();
        HeldCollectionPageQueryRequest request = new HeldCollectionPageQueryRequest();
        request.setUserId(userId);
        request.setKeyword(keyword);
        request.setState(state);
        request.setCurrentPage(currentPage);
        request.setPageSize(pageSize);
        PageResponse<HeldCollectionVO> response = collectionReadFacadeService.pageQueryHeldCollection(request);
        return MultiResult.successMulti(response.getDatas(), response.getTotal(), response.getCurrentPage(), response.getPageSize());
    }

    // 查询现有持有藏品数量
    @GetMapping("/heldCollectionCount")
    public Result<Long> heldCollectionCount(){
        String userId = (String) StpUtil.getLoginId();
        SingleResponse<Long> response = collectionReadFacadeService.queryHeldCollectionCount(userId);
        return Result.success(response.getData());
    }

    // 查询累计持有藏品数量
    @GetMapping("/heldCollectionAllCount")
    public Result<Long> heldCollectionAllCount(){
        String userId = (String) StpUtil.getLoginId();
        SingleResponse<Long> response = collectionReadFacadeService.queryHeldCollectionAllCount(userId);
        return Result.success(response.getData());
    }

    // 查询用户当前持仓总铸造值
    @GetMapping("/userForgeValue")
    public Result<Long> userForgeValue(){
        String userId = (String) StpUtil.getLoginId();
        SingleResponse<Long> response = collectionReadFacadeService.queryUserForgeValue(userId);
        return Result.success(response.getData());
    }

    // 用户持有藏品详情
    @GetMapping("/heldCollectionInfo")
    public Result<HeldCollectionVO> heldCollectionInfo(@NotBlank String heldCollectionId){
        SingleResponse<HeldCollectionVO> response = collectionReadFacadeService.queryHeldCollectionById(Long.valueOf(heldCollectionId));
        return Result.success(response.getData());
    }

    // 藏品转让
    @PostMapping("/transfer")
    public Result<Boolean> transfer(@Valid @RequestBody TransferParam param) {
        String userId = (String) StpUtil.getLoginId();
        if (userId.equals(param.getRecipientUserId())) {
            throw new CollectionException(TRANSFER_SELF_ERROR);
        }
        SingleResponse<HeldCollectionVO> response = collectionReadFacadeService.queryHeldCollectionById(Long.parseLong(param.getHeldCollectionId()));
        // 查询转让藏品
        HeldCollectionVO heldCollection = response.getData();
        Assert.notNull(heldCollection,() -> new CollectionException(HELD_COLLECTION_QUERY_FAIL));
        UserQueryRequest userQueryRequest = new UserQueryRequest(Long.valueOf(param.getRecipientUserId()));

        // 查询接受者
        UserQueryResponse<UserInfo> userQueryResponse = userFacadeService.query(userQueryRequest);
        if (!userQueryResponse.getSuccess() || userQueryResponse.getData() == null) {
            throw new CollectionException(USER_NOT_EXIST);
        }
        UserInfo recipient = userQueryResponse.getData();
        if (!recipient.userCanBuy()) {
            throw new CollectionException(BUYER_STATUS_ABNORMAL);
        }

        Assert.isTrue(StringUtils.equals(heldCollection.getUserId(), userId), () -> new CollectionException(HELD_COLLECTION_OWNER_CHECK_ERROR));

        // 1.本地数据先变更
        HeldCollectionTransferRequest transferRequest = new HeldCollectionTransferRequest();
        transferRequest.setRecipientUserId(param.getRecipientUserId());
        transferRequest.setHeldCollectionId(param.getHeldCollectionId());
        transferRequest.setOperatorId(userId);
        transferRequest.setIdentifier(param.getHeldCollectionId() + "_TRANSFER");
        HeldCollection transferHeldCollection = heldCollectionService.transfer(transferRequest);
        Assert.notNull(transferHeldCollection, () -> new CollectionException(HELD_COLLECTION_SAVE_FAILED));

        // 2.链操作
        ChainProcessRequest request = new ChainProcessRequest();
        request.setBizId(String.valueOf(transferHeldCollection.getId()));
        request.setBizType(ChainOperateBizTypeEnum.HELD_COLLECTION.name());
        request.setIdentifier(param.getHeldCollectionId() + SEPARATOR + param.getRecipientUserId() + SEPARATOR + ChainOperateTypeEnum.COLLECTION_TRANSFER.name());
        UserInfo owner = (UserInfo) StpUtil.getSession().get(userId);
        request.setOwner(owner.getBlockChainUrl());
        request.setClassId(String.valueOf(heldCollection.getCollectionId()));
        request.setNtfId(transferHeldCollection.getNftId());
        request.setRecipient(recipient.getBlockChainUrl());
        var res = chainFacadeService.transfer(request);
        return Result.success(res.getSuccess());
    }
    // 藏品销毁
    @PostMapping("/destroy")
    public Result<Boolean> destroy(@Valid @RequestBody DestroyParam destroyParam){
        String userId = (String) StpUtil.getLoginId();

        HeldCollectionDestroyRequest request = new HeldCollectionDestroyRequest();
        request.setOperatorId(userId);
        request.setHeldCollectionId(destroyParam.getHeldCollectionId());
        request.setIdentifier(destroyParam.getHeldCollectionId() + "_DESTROY");
        // 1.销毁藏品
        HeldCollection heldCollection = heldCollectionService.destroy(request);

        // 2.链操作
        if(null != heldCollection){
            ChainProcessRequest chainProcessRequest = new ChainProcessRequest();
            chainProcessRequest.setBizId(String.valueOf(destroyParam.getHeldCollectionId()));
            chainProcessRequest.setBizType(ChainOperateBizTypeEnum.HELD_COLLECTION.name());
            chainProcessRequest.setIdentifier(destroyParam.getHeldCollectionId() + SEPARATOR + ChainOperateBizTypeEnum.HELD_COLLECTION.name());
            UserInfo owner = (UserInfo) StpUtil.getSession().get(userId);
            chainProcessRequest.setOwner(owner.getBlockChainUrl());
            chainProcessRequest.setClassId(String.valueOf(heldCollection.getCollectionId()));
            chainProcessRequest.setNtfId(heldCollection.getNftId());
            var response = chainFacadeService.destroy(chainProcessRequest);
            return Result.success(response.getSuccess());
        }
        return Result.success(false);
    }

    /** 铸造值排行榜 TopN */
    @GetMapping("/forgeRank")
    public MultiResult<ForgeRankVO> forgeRank(@RequestParam(defaultValue = "20") int topN) {
        List<ForgeRankVO> list = heldCollectionService.getForgeRankTopN(topN);
        // 批量填充昵称
        if (!list.isEmpty()) {
            for (ForgeRankVO vo : list) {
                UserQueryRequest userQuery = new UserQueryRequest(Long.valueOf(vo.getUserId()));
                UserQueryResponse<UserInfo> resp = userFacadeService.query(userQuery);
                if (resp.getSuccess() && resp.getData() != null) {
                    vo.setNickName(resp.getData().getNickName());
                }
            }
        }
        return MultiResult.successMulti(list, list.size(), topN, topN);
    }

    /** 我的铸造值排名 */
    @GetMapping("/myForgeRank")
    public Result<Integer> myForgeRank() {
        String userId = (String) StpUtil.getLoginId();
        Integer rank = heldCollectionService.getMyForgeRank(userId);
        return Result.success(rank);
    }
}