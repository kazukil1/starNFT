package cn.kaziki.nft.turbo.trade.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.kaziki.nft.turbo.api.order.constant.TradeOrderState;
import cn.kaziki.nft.turbo.api.order.model.TradeOrderVO;
import cn.kaziki.nft.turbo.api.order.request.OrderPageQueryRequest;
import cn.kaziki.nft.turbo.api.order.request.OrderTimeoutRequest;
import cn.kaziki.nft.turbo.api.order.service.OrderFacadeService;
import cn.kaziki.nft.turbo.api.pay.model.PayOrderVO;
import cn.kaziki.nft.turbo.api.pay.service.PayFacadeService;
import cn.kaziki.nft.turbo.api.user.constant.UserType;
import cn.kaziki.nft.turbo.base.response.PageResponse;
import cn.kaziki.nft.turbo.base.response.SingleResponse;
import cn.kaziki.nft.turbo.web.util.MultiResultConvertor;
import cn.kaziki.nft.turbo.web.vo.MultiResult;
import cn.kaziki.nft.turbo.web.vo.Result;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("order")
public class OrderController {
    @Autowired
    private OrderFacadeService orderFacadeService;

    @Autowired
    private PayFacadeService payFacadeService;

    // 订单列表分页
    @GetMapping("/orderList")
    public MultiResult<TradeOrderVO> orderList(String state, int pageSize, int currentPage){
        String userId = (String) StpUtil.getLoginId();
        OrderPageQueryRequest request = new OrderPageQueryRequest();
        request.setBuyerId(userId);
        request.setState(state);
        request.setPageSize(pageSize);
        request.setCurrentPage(currentPage);
        PageResponse<TradeOrderVO> response = orderFacadeService.pageQuery(request);
        return MultiResultConvertor.convert(response);
    }

    // 订单详情
    @GetMapping("/orderDetail")
    public Result<TradeOrderVO> orderDetail(@NotNull String orderId){
        String userId = (String) StpUtil.getLoginId();
        SingleResponse<TradeOrderVO> singleResponse = orderFacadeService.getTradeOrder(orderId, userId);
        if(singleResponse.getSuccess()){
            TradeOrderVO tradeOrderVO = singleResponse.getData();
            if(tradeOrderVO == null){
                // 异步创建订单场景下，MQ 尚未消费完成时订单暂不存在，返回空数据而非报错，前端轮询静默重试
                return Result.success(null);
            }
            //如果订单已经超时，并且尚未关闭，则执行一次关单后再返回数据
            if(tradeOrderVO.getTimeout() && tradeOrderVO.getOrderState() == TradeOrderState.CONFIRM){
                OrderTimeoutRequest request = new OrderTimeoutRequest();
                request.setOperatorType(UserType.PLATFORM);
                request.setOperator(UserType.PLATFORM.getDesc());
                request.setOrderId(orderId);
                request.setOperateTime(new Date());
                request.setIdentifier(UUID.randomUUID().toString());
                orderFacadeService.timeout(request);
                singleResponse = orderFacadeService.getTradeOrder(orderId,userId);
            }
            return Result.success(singleResponse.getData());
        }else {
            return Result.error(singleResponse.getResponseCode(),singleResponse.getResponseMessage());
        }
    }

    // 订单列表
    @GetMapping("/getPayStatus")
    public Result<PayOrderVO> getPayStatus(@NotNull String payOrderId) {
        String userId = (String) StpUtil.getLoginId();
        SingleResponse<PayOrderVO> singleResponse = payFacadeService.queryPayOrder(payOrderId, userId);
        return new Result(singleResponse);
    }

}
