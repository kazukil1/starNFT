package cn.kaziki.nft.turbo.trade.application;

import cn.kaziki.nft.turbo.api.goods.constant.GoodsType;
import cn.kaziki.nft.turbo.api.inventory.request.InventoryRequest;
import cn.kaziki.nft.turbo.api.inventory.service.InventoryFacadeService;
import cn.kaziki.nft.turbo.api.order.request.OrderCreateAndConfirmRequest;
import cn.kaziki.nft.turbo.api.order.response.OrderResponse;
import cn.kaziki.nft.turbo.api.order.service.OrderFacadeService;
import cn.kaziki.nft.turbo.base.response.SingleResponse;
import cn.kaziki.turbo.stream.producer.StreamProducer;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TradeApplicationService 单元测试 — newBuyPlus 非TCC下单
 */
public class TradeApplicationServiceTest {

    private TradeApplicationService service;
    private InventoryFacadeService inventoryFacadeService;
    private OrderFacadeService orderFacadeService;
    private StreamProducer streamProducer;

    @Before
    public void setUp() throws Exception {
        inventoryFacadeService = mock(InventoryFacadeService.class);
        orderFacadeService = mock(OrderFacadeService.class);
        streamProducer = mock(StreamProducer.class);

        service = new TradeApplicationService();
        setField(service, "inventoryFacadeService", inventoryFacadeService);
        setField(service, "orderFacadeService", orderFacadeService);
        setField(service, "streamProducer", streamProducer);
    }

    // ==================== newBuyPlus — 成功 ====================

    @Test
    public void testNewBuyPlus_Success_ReturnsSuccess() {
        OrderCreateAndConfirmRequest request = buildRequest();

        // Redis 库存扣减成功
        when(inventoryFacadeService.decrease(any(InventoryRequest.class)))
                .thenReturn(SingleResponse.of(true));
        // DB 创建订单成功
        OrderResponse createResp = new OrderResponse.OrderResponseBuilder()
                .orderId("order-001").buildSuccess();
        when(orderFacadeService.createAndConfirm(any(OrderCreateAndConfirmRequest.class)))
                .thenReturn(createResp);

        OrderResponse result = service.newBuyPlus(request);

        assertTrue(result.getSuccess());
        assertEquals("order-001", result.getOrderId());
        // 不发送补偿消息
        verify(streamProducer, never()).send(anyString(), anyString(), anyString(), anyInt());
    }

    // ==================== newBuyPlus — Redis扣减失败 → 查日志 ====================

    @Test
    public void testNewBuyPlus_RedisDecreaseFail_LogFound_Continues() {
        OrderCreateAndConfirmRequest request = buildRequest();

        // Redis 扣减失败
        when(inventoryFacadeService.decrease(any(InventoryRequest.class)))
                .thenReturn(SingleResponse.fail("ERROR", "库存不足"));
        // 日志存在（说明之前已扣减），继续
        when(inventoryFacadeService.getInventoryDecreaseLog(any(InventoryRequest.class)))
                .thenReturn(SingleResponse.of("log-found"));

        OrderResponse createResp = new OrderResponse.OrderResponseBuilder()
                .orderId("order-001").buildSuccess();
        when(orderFacadeService.createAndConfirm(any(OrderCreateAndConfirmRequest.class)))
                .thenReturn(createResp);

        OrderResponse result = service.newBuyPlus(request);
        assertTrue(result.getSuccess());
    }

    // ==================== newBuyPlus — Redis扣减失败 + 日志不存在 → 失败 ====================

    @Test
    public void testNewBuyPlus_RedisDecreaseFail_LogNotFound_ReturnsFail() {
        OrderCreateAndConfirmRequest request = buildRequest();

        when(inventoryFacadeService.decrease(any(InventoryRequest.class)))
                .thenReturn(SingleResponse.fail("ERROR", "库存不足"));
        when(inventoryFacadeService.getInventoryDecreaseLog(any(InventoryRequest.class)))
                .thenReturn(SingleResponse.fail("NOT_FOUND", "未找到"));

        OrderResponse result = service.newBuyPlus(request);

        assertFalse(result.getSuccess());
        // 不继续下单
        verify(orderFacadeService, never()).createAndConfirm(any());
    }

    // ==================== newBuyPlus — 创建订单失败 → 发送补偿消息 ====================

    @Test
    public void testNewBuyPlus_OrderCreateFail_SendsPreCancelMessage() {
        OrderCreateAndConfirmRequest request = buildRequest();

        when(inventoryFacadeService.decrease(any(InventoryRequest.class)))
                .thenReturn(SingleResponse.of(true));
        // 创建订单失败
        when(orderFacadeService.createAndConfirm(any(OrderCreateAndConfirmRequest.class)))
                .thenReturn(new OrderResponse.OrderResponseBuilder().buildFail("FAIL", "创建失败"));
        when(streamProducer.send(anyString(), anyString(), anyString(), anyInt())).thenReturn(true);

        OrderResponse result = service.newBuyPlus(request);

        assertFalse(result.getSuccess());
        // 发送了延迟 30 秒的补偿消息（DELAY_LEVEL_30_S = 4）
        verify(streamProducer, times(1)).send(
                eq("newBuyPlusPreCancel-out-0"), anyString(), anyString(), eq(4));
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

    private OrderCreateAndConfirmRequest buildRequest() {
        OrderCreateAndConfirmRequest request = new OrderCreateAndConfirmRequest();
        request.setOrderId("order-001");
        request.setBuyerId("user-001");
        request.setGoodsId("100");
        request.setGoodsType(GoodsType.COLLECTION);
        request.setItemCount(1L);
        request.setIdentifier("idempotent-001");
        request.setSyncDecreaseInventory(true);
        return request;
    }
}
