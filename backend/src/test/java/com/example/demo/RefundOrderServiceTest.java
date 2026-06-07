package com.example.demo;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.dto.RefundApplyDTO;
import com.example.demo.dto.RefundAuditDTO;
import com.example.demo.entity.*;
import com.example.demo.enums.RefundStatusEnum;
import com.example.demo.enums.StockReservationStatusEnum;
import com.example.demo.mapper.*;
import com.example.demo.service.RefundOrderService;
import com.example.demo.util.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class RefundOrderServiceTest {

    @Autowired
    private RefundOrderService refundOrderService;

    @Autowired
    private RefundOrderMapper refundOrderMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private StockReservationMapper stockReservationMapper;

    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_PRODUCT_ID = 1L;

    @BeforeEach
    void setUp() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");
        }
        ensureTestProductExists();
    }

    @Test
    @DisplayName("申请退款 - 正常流程（已支付订单）")
    void testApplyRefund_Success_PaidOrder() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            Order order = createTestOrder(TEST_USER_ID, 1);

            RefundApplyDTO dto = new RefundApplyDTO();
            dto.setOrderId(order.getId());
            dto.setRefundAmount(new BigDecimal("99.99"));
            dto.setRefundType(1);
            dto.setRefundReason("商品质量问题");
            dto.setRefundDesc("测试退款描述");

            RefundOrder refundOrder = refundOrderService.applyRefund(dto);

            assertNotNull(refundOrder.getId());
            assertEquals(order.getId(), refundOrder.getOrderId());
            assertEquals(TEST_USER_ID, refundOrder.getUserId());
            assertNotNull(refundOrder.getRefundNo());
            assertTrue(refundOrder.getRefundNo().startsWith("REF"));
            assertEquals(0, new BigDecimal("99.99").compareTo(refundOrder.getRefundAmount()));
            assertEquals(RefundStatusEnum.PENDING.getCode(), refundOrder.getStatus());
            assertEquals(1, refundOrder.getOriginalOrderStatus());

            Order updatedOrder = orderMapper.selectById(order.getId());
            assertEquals(5, updatedOrder.getStatus());
        }
    }

    @Test
    @DisplayName("申请退款 - 正常流程（已发货订单）")
    void testApplyRefund_Success_ShippedOrder() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            Order order = createTestOrder(TEST_USER_ID, 2);

            RefundApplyDTO dto = new RefundApplyDTO();
            dto.setOrderId(order.getId());
            dto.setRefundAmount(new BigDecimal("99.99"));
            dto.setRefundType(2);
            dto.setRefundReason("不想要了");

            RefundOrder refundOrder = refundOrderService.applyRefund(dto);

            assertNotNull(refundOrder.getId());
            assertEquals(2, refundOrder.getOriginalOrderStatus());

            Order updatedOrder = orderMapper.selectById(order.getId());
            assertEquals(5, updatedOrder.getStatus());
        }
    }

    @Test
    @DisplayName("申请退款 - 正常流程（已完成订单）")
    void testApplyRefund_Success_CompletedOrder() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            Order order = createTestOrder(TEST_USER_ID, 3);

            RefundApplyDTO dto = new RefundApplyDTO();
            dto.setOrderId(order.getId());
            dto.setRefundAmount(new BigDecimal("99.99"));
            dto.setRefundType(1);
            dto.setRefundReason("商品与描述不符");

            RefundOrder refundOrder = refundOrderService.applyRefund(dto);

            assertNotNull(refundOrder.getId());
            assertEquals(3, refundOrder.getOriginalOrderStatus());
        }
    }

    @Test
    @DisplayName("申请退款 - 订单不存在失败")
    void testApplyRefund_OrderNotFound() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            RefundApplyDTO dto = new RefundApplyDTO();
            dto.setOrderId(99999L);
            dto.setRefundAmount(new BigDecimal("99.99"));
            dto.setRefundType(1);
            dto.setRefundReason("测试");

            assertThrows(IllegalArgumentException.class, () ->
                refundOrderService.applyRefund(dto));
        }
    }

    @Test
    @DisplayName("申请退款 - 无权申请他人订单退款失败")
    void testApplyRefund_NoPermission() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            Order order = createTestOrder(999L, 1);

            RefundApplyDTO dto = new RefundApplyDTO();
            dto.setOrderId(order.getId());
            dto.setRefundAmount(new BigDecimal("99.99"));
            dto.setRefundType(1);
            dto.setRefundReason("测试");

            assertThrows(SecurityException.class, () ->
                refundOrderService.applyRefund(dto));
        }
    }

    @Test
    @DisplayName("申请退款 - 订单状态不允许失败（待支付）")
    void testApplyRefund_InvalidStatus_Pending() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            Order order = createTestOrder(TEST_USER_ID, 0);

            RefundApplyDTO dto = new RefundApplyDTO();
            dto.setOrderId(order.getId());
            dto.setRefundAmount(new BigDecimal("99.99"));
            dto.setRefundType(1);
            dto.setRefundReason("测试");

            assertThrows(IllegalArgumentException.class, () ->
                refundOrderService.applyRefund(dto));
        }
    }

    @Test
    @DisplayName("申请退款 - 订单状态不允许失败（已取消）")
    void testApplyRefund_InvalidStatus_Cancelled() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            Order order = createTestOrder(TEST_USER_ID, 4);

            RefundApplyDTO dto = new RefundApplyDTO();
            dto.setOrderId(order.getId());
            dto.setRefundAmount(new BigDecimal("99.99"));
            dto.setRefundType(1);
            dto.setRefundReason("测试");

            assertThrows(IllegalArgumentException.class, () ->
                refundOrderService.applyRefund(dto));
        }
    }

    @Test
    @DisplayName("申请退款 - 订单状态不允许失败（退款中）")
    void testApplyRefund_InvalidStatus_Refunding() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            Order order = createTestOrder(TEST_USER_ID, 5);

            RefundApplyDTO dto = new RefundApplyDTO();
            dto.setOrderId(order.getId());
            dto.setRefundAmount(new BigDecimal("99.99"));
            dto.setRefundType(1);
            dto.setRefundReason("测试");

            assertThrows(IllegalArgumentException.class, () ->
                refundOrderService.applyRefund(dto));
        }
    }

    @Test
    @DisplayName("申请退款 - 已有待审核退款申请失败")
    void testApplyRefund_AlreadyPending() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            Order order = createTestOrder(TEST_USER_ID, 1);

            RefundOrder existingRefund = new RefundOrder();
            existingRefund.setOrderId(order.getId());
            existingRefund.setUserId(TEST_USER_ID);
            existingRefund.setRefundNo("REFTEST001");
            existingRefund.setRefundAmount(new BigDecimal("99.99"));
            existingRefund.setRefundType(1);
            existingRefund.setRefundReason("测试");
            existingRefund.setStatus(RefundStatusEnum.PENDING.getCode());
            existingRefund.setOriginalOrderStatus(1);
            refundOrderMapper.insert(existingRefund);

            RefundApplyDTO dto = new RefundApplyDTO();
            dto.setOrderId(order.getId());
            dto.setRefundAmount(new BigDecimal("99.99"));
            dto.setRefundType(1);
            dto.setRefundReason("测试2");

            assertThrows(IllegalArgumentException.class, () ->
                refundOrderService.applyRefund(dto));
        }
    }

    @Test
    @DisplayName("申请退款 - 退款金额超过订单金额失败")
    void testApplyRefund_AmountExceed() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            Order order = createTestOrder(TEST_USER_ID, 1);

            RefundApplyDTO dto = new RefundApplyDTO();
            dto.setOrderId(order.getId());
            dto.setRefundAmount(new BigDecimal("999.99"));
            dto.setRefundType(1);
            dto.setRefundReason("测试");

            assertThrows(IllegalArgumentException.class, () ->
                refundOrderService.applyRefund(dto));
        }
    }

    @Test
    @DisplayName("申请退款 - 事务回滚测试：创建退款成功但订单状态更新失败时回滚")
    void testApplyRefund_TransactionRollback() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            Order order = createTestOrder(TEST_USER_ID, 99);

            RefundApplyDTO dto = new RefundApplyDTO();
            dto.setOrderId(order.getId());
            dto.setRefundAmount(new BigDecimal("99.99"));
            dto.setRefundType(1);
            dto.setRefundReason("测试");

            assertThrows(IllegalArgumentException.class, () ->
                refundOrderService.applyRefund(dto));

            List<RefundOrder> refunds = refundOrderMapper.selectList(
                new LambdaQueryWrapper<RefundOrder>().eq(RefundOrder::getOrderId, order.getId())
            );
            assertEquals(0, refunds.size());
        }
    }

    @Test
    @DisplayName("审核退款 - 审核通过（未扣减库存的订单）")
    void testAuditRefund_Approve_NotDeducted() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("admin");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("ADMIN");

            Order order = createTestOrder(TEST_USER_ID, 5);
            order.setStatus(5);
            orderMapper.updateById(order);

            Product productBefore = productMapper.selectById(TEST_PRODUCT_ID);
            productBefore.setReservedStock(2);
            productMapper.updateById(productBefore);

            createTestOrderItem(order.getId(), TEST_PRODUCT_ID, 2);

            StockReservation reservation = new StockReservation();
            reservation.setOrderId(order.getId());
            reservation.setOrderItemId(1L);
            reservation.setProductId(TEST_PRODUCT_ID);
            reservation.setProductName("测试商品");
            reservation.setQuantity(2);
            reservation.setStatus(StockReservationStatusEnum.RESERVED.getCode());
            reservation.setExpireTime(java.time.LocalDateTime.now().plusDays(1));
            stockReservationMapper.insert(reservation);

            RefundOrder refundOrder = new RefundOrder();
            refundOrder.setOrderId(order.getId());
            refundOrder.setUserId(TEST_USER_ID);
            refundOrder.setRefundNo("REFTEST002");
            refundOrder.setRefundAmount(new BigDecimal("199.98"));
            refundOrder.setRefundType(1);
            refundOrder.setRefundReason("测试");
            refundOrder.setStatus(RefundStatusEnum.PENDING.getCode());
            refundOrder.setOriginalOrderStatus(1);
            refundOrderMapper.insert(refundOrder);

            RefundAuditDTO dto = new RefundAuditDTO();
            dto.setRefundId(refundOrder.getId());
            dto.setApproved(true);
            dto.setAuditRemark("同意退款");

            refundOrderService.auditRefund(dto);

            RefundOrder updatedRefund = refundOrderMapper.selectById(refundOrder.getId());
            assertEquals(RefundStatusEnum.APPROVED.getCode(), updatedRefund.getStatus());
            assertNotNull(updatedRefund.getAuditTime());
            assertEquals("同意退款", updatedRefund.getAuditRemark());
            assertEquals(TEST_USER_ID, updatedRefund.getAuditUserId());

            Order updatedOrder = orderMapper.selectById(order.getId());
            assertEquals(4, updatedOrder.getStatus());

            StockReservation updatedReservation = stockReservationMapper.selectById(reservation.getId());
            assertEquals(StockReservationStatusEnum.RELEASED.getCode(), updatedReservation.getStatus());

            Product productAfter = productMapper.selectById(TEST_PRODUCT_ID);
            assertEquals(0, productAfter.getReservedStock());
        }
    }

    @Test
    @DisplayName("审核退款 - 审核通过（已扣减库存的订单，需要回滚库存）")
    void testAuditRefund_Approve_Deducted() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("admin");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("ADMIN");

            Order order = createTestOrder(TEST_USER_ID, 5);
            order.setStatus(5);
            orderMapper.updateById(order);

            Product productBefore = productMapper.selectById(TEST_PRODUCT_ID);
            int initialStock = productBefore.getStock();
            productBefore.setReservedStock(0);
            productMapper.updateById(productBefore);

            createTestOrderItem(order.getId(), TEST_PRODUCT_ID, 3);

            StockReservation reservation = new StockReservation();
            reservation.setOrderId(order.getId());
            reservation.setOrderItemId(1L);
            reservation.setProductId(TEST_PRODUCT_ID);
            reservation.setProductName("测试商品");
            reservation.setQuantity(3);
            reservation.setStatus(StockReservationStatusEnum.DEDUCTED.getCode());
            reservation.setExpireTime(java.time.LocalDateTime.now().plusDays(1));
            stockReservationMapper.insert(reservation);

            RefundOrder refundOrder = new RefundOrder();
            refundOrder.setOrderId(order.getId());
            refundOrder.setUserId(TEST_USER_ID);
            refundOrder.setRefundNo("REFTEST003");
            refundOrder.setRefundAmount(new BigDecimal("299.97"));
            refundOrder.setRefundType(1);
            refundOrder.setRefundReason("测试");
            refundOrder.setStatus(RefundStatusEnum.PENDING.getCode());
            refundOrder.setOriginalOrderStatus(3);
            refundOrderMapper.insert(refundOrder);

            RefundAuditDTO dto = new RefundAuditDTO();
            dto.setRefundId(refundOrder.getId());
            dto.setApproved(true);
            dto.setAuditRemark("同意退款");

            refundOrderService.auditRefund(dto);

            RefundOrder updatedRefund = refundOrderMapper.selectById(refundOrder.getId());
            assertEquals(RefundStatusEnum.APPROVED.getCode(), updatedRefund.getStatus());

            Order updatedOrder = orderMapper.selectById(order.getId());
            assertEquals(4, updatedOrder.getStatus());

            Product productAfter = productMapper.selectById(TEST_PRODUCT_ID);
            assertEquals(initialStock + 3, productAfter.getStock());
        }
    }

    @Test
    @DisplayName("审核退款 - 审核拒绝")
    void testAuditRefund_Reject() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("admin");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("ADMIN");

            Order order = createTestOrder(TEST_USER_ID, 5);
            order.setStatus(5);
            orderMapper.updateById(order);

            RefundOrder refundOrder = new RefundOrder();
            refundOrder.setOrderId(order.getId());
            refundOrder.setUserId(TEST_USER_ID);
            refundOrder.setRefundNo("REFTEST004");
            refundOrder.setRefundAmount(new BigDecimal("99.99"));
            refundOrder.setRefundType(1);
            refundOrder.setRefundReason("测试");
            refundOrder.setStatus(RefundStatusEnum.PENDING.getCode());
            refundOrder.setOriginalOrderStatus(1);
            refundOrderMapper.insert(refundOrder);

            RefundAuditDTO dto = new RefundAuditDTO();
            dto.setRefundId(refundOrder.getId());
            dto.setApproved(false);
            dto.setAuditRemark("商品已使用，不支持退款");

            refundOrderService.auditRefund(dto);

            RefundOrder updatedRefund = refundOrderMapper.selectById(refundOrder.getId());
            assertEquals(RefundStatusEnum.REJECTED.getCode(), updatedRefund.getStatus());
            assertNotNull(updatedRefund.getAuditTime());
            assertEquals("商品已使用，不支持退款", updatedRefund.getAuditRemark());

            Order updatedOrder = orderMapper.selectById(order.getId());
            assertEquals(1, updatedOrder.getStatus());
        }
    }

    @Test
    @DisplayName("审核退款 - 非管理员权限失败")
    void testAuditRefund_NonAdminFailure() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            RefundAuditDTO dto = new RefundAuditDTO();
            dto.setRefundId(1L);
            dto.setApproved(true);
            dto.setAuditRemark("同意退款");

            assertThrows(SecurityException.class, () ->
                refundOrderService.auditRefund(dto));
        }
    }

    @Test
    @DisplayName("审核退款 - 退款单不存在失败")
    void testAuditRefund_NotFound() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("admin");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("ADMIN");

            RefundAuditDTO dto = new RefundAuditDTO();
            dto.setRefundId(99999L);
            dto.setApproved(true);
            dto.setAuditRemark("同意退款");

            assertThrows(IllegalArgumentException.class, () ->
                refundOrderService.auditRefund(dto));
        }
    }

    @Test
    @DisplayName("审核退款 - 退款单状态不允许审核失败")
    void testAuditRefund_InvalidStatus() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("admin");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("ADMIN");

            RefundOrder refundOrder = new RefundOrder();
            refundOrder.setOrderId(1L);
            refundOrder.setUserId(TEST_USER_ID);
            refundOrder.setRefundNo("REFTEST005");
            refundOrder.setRefundAmount(new BigDecimal("99.99"));
            refundOrder.setRefundType(1);
            refundOrder.setRefundReason("测试");
            refundOrder.setStatus(RefundStatusEnum.APPROVED.getCode());
            refundOrder.setOriginalOrderStatus(1);
            refundOrderMapper.insert(refundOrder);

            RefundAuditDTO dto = new RefundAuditDTO();
            dto.setRefundId(refundOrder.getId());
            dto.setApproved(true);
            dto.setAuditRemark("同意退款");

            assertThrows(IllegalArgumentException.class, () ->
                refundOrderService.auditRefund(dto));
        }
    }

    @Test
    @DisplayName("取消退款申请 - 正常流程")
    void testCancelRefund_Success() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            Order order = createTestOrder(TEST_USER_ID, 5);
            order.setStatus(5);
            orderMapper.updateById(order);

            RefundOrder refundOrder = new RefundOrder();
            refundOrder.setOrderId(order.getId());
            refundOrder.setUserId(TEST_USER_ID);
            refundOrder.setRefundNo("REFTEST006");
            refundOrder.setRefundAmount(new BigDecimal("99.99"));
            refundOrder.setRefundType(1);
            refundOrder.setRefundReason("测试");
            refundOrder.setStatus(RefundStatusEnum.PENDING.getCode());
            refundOrder.setOriginalOrderStatus(1);
            refundOrderMapper.insert(refundOrder);

            refundOrderService.cancelRefund(refundOrder.getId());

            RefundOrder updatedRefund = refundOrderMapper.selectById(refundOrder.getId());
            assertEquals(RefundStatusEnum.CANCELLED.getCode(), updatedRefund.getStatus());

            Order updatedOrder = orderMapper.selectById(order.getId());
            assertEquals(1, updatedOrder.getStatus());
        }
    }

    @Test
    @DisplayName("取消退款申请 - 无权取消他人申请失败")
    void testCancelRefund_NoPermission() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            RefundOrder refundOrder = new RefundOrder();
            refundOrder.setOrderId(1L);
            refundOrder.setUserId(999L);
            refundOrder.setRefundNo("REFTEST007");
            refundOrder.setRefundAmount(new BigDecimal("99.99"));
            refundOrder.setRefundType(1);
            refundOrder.setRefundReason("测试");
            refundOrder.setStatus(RefundStatusEnum.PENDING.getCode());
            refundOrder.setOriginalOrderStatus(1);
            refundOrderMapper.insert(refundOrder);

            assertThrows(SecurityException.class, () ->
                refundOrderService.cancelRefund(refundOrder.getId()));
        }
    }

    @Test
    @DisplayName("取消退款申请 - 状态不允许失败")
    void testCancelRefund_InvalidStatus() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            RefundOrder refundOrder = new RefundOrder();
            refundOrder.setOrderId(1L);
            refundOrder.setUserId(TEST_USER_ID);
            refundOrder.setRefundNo("REFTEST008");
            refundOrder.setRefundAmount(new BigDecimal("99.99"));
            refundOrder.setRefundType(1);
            refundOrder.setRefundReason("测试");
            refundOrder.setStatus(RefundStatusEnum.APPROVED.getCode());
            refundOrder.setOriginalOrderStatus(1);
            refundOrderMapper.insert(refundOrder);

            assertThrows(IllegalArgumentException.class, () ->
                refundOrderService.cancelRefund(refundOrder.getId()));
        }
    }

    @Test
    @DisplayName("获取退款详情 - 正常流程")
    void testGetRefundDetail_Success() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            Order order = createTestOrder(TEST_USER_ID, 1);

            RefundOrder refundOrder = new RefundOrder();
            refundOrder.setOrderId(order.getId());
            refundOrder.setUserId(TEST_USER_ID);
            refundOrder.setRefundNo("REFTEST009");
            refundOrder.setRefundAmount(new BigDecimal("99.99"));
            refundOrder.setRefundType(1);
            refundOrder.setRefundReason("测试");
            refundOrder.setStatus(RefundStatusEnum.PENDING.getCode());
            refundOrder.setOriginalOrderStatus(1);
            refundOrderMapper.insert(refundOrder);

            var detail = refundOrderService.getRefundDetail(refundOrder.getId());

            assertNotNull(detail);
            assertEquals(refundOrder.getId(), detail.getId());
            assertEquals("REFTEST009", detail.getRefundNo());
        }
    }

    @Test
    @DisplayName("获取退款详情 - 无权查看他人退款单失败")
    void testGetRefundDetail_NoPermission() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            RefundOrder refundOrder = new RefundOrder();
            refundOrder.setOrderId(1L);
            refundOrder.setUserId(999L);
            refundOrder.setRefundNo("REFTEST010");
            refundOrder.setRefundAmount(new BigDecimal("99.99"));
            refundOrder.setRefundType(1);
            refundOrder.setRefundReason("测试");
            refundOrder.setStatus(RefundStatusEnum.PENDING.getCode());
            refundOrder.setOriginalOrderStatus(1);
            refundOrderMapper.insert(refundOrder);

            assertThrows(SecurityException.class, () ->
                refundOrderService.getRefundDetail(refundOrder.getId()));
        }
    }

    @Test
    @DisplayName("分页查询退款单 - 普通用户权限")
    void testPageRefunds_UserPermission() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            var page = refundOrderService.pageRefunds(1, 10, null, null, null, null);

            assertNotNull(page);
            assertTrue(page.getRecords().size() <= 10);
        }
    }

    @Test
    @DisplayName("分页查询退款单 - 管理员权限")
    void testPageRefunds_AdminPermission() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("admin");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("ADMIN");

            var page = refundOrderService.pageRefunds(1, 10, null, null, null, null);

            assertNotNull(page);
            assertTrue(page.getRecords().size() <= 10);
        }
    }

    private void ensureTestProductExists() {
        Product product = productMapper.selectById(TEST_PRODUCT_ID);
        if (product == null) {
            product = new Product();
            product.setId(TEST_PRODUCT_ID);
            product.setName("测试商品");
            product.setPrice(new BigDecimal("99.99"));
            product.setStock(100);
            product.setReservedStock(0);
            product.setCategory("测试分类");
            productMapper.insert(product);
        } else {
            product.setStock(100);
            product.setReservedStock(0);
            productMapper.updateById(product);
        }
    }

    private Order createTestOrder(Long userId, Integer status) {
        Order order = new Order();
        order.setUserId(userId);
        order.setTotalAmount(new BigDecimal("99.99"));
        order.setStatus(status);
        order.setVersion(1);
        orderMapper.insert(order);
        return order;
    }

    private void createTestOrderItem(Long orderId, Long productId, Integer quantity) {
        OrderItem item = new OrderItem();
        item.setOrderId(orderId);
        item.setProductId(productId);
        item.setProductName("测试商品");
        item.setQuantity(quantity);
        item.setPrice(new BigDecimal("99.99"));
        orderItemMapper.insert(item);
    }
}
