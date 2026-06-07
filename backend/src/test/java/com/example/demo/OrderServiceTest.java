package com.example.demo;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.entity.*;
import com.example.demo.enums.StockReservationStatusEnum;
import com.example.demo.mapper.*;
import com.example.demo.service.OrderService;
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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private UserAddressMapper userAddressMapper;

    @Autowired
    private StockReservationMapper stockReservationMapper;

    @Autowired
    private CouponTemplateMapper couponTemplateMapper;

    @Autowired
    private UserCouponMapper userCouponMapper;

    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_PRODUCT_ID = 1L;
    private static final Long TEST_ADDRESS_ID = 1L;

    @BeforeEach
    void setUp() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");
        }
    }

    @Test
    @DisplayName("创建订单 - 正常流程")
    void testCreateOrder_Success() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            ensureTestDataExists();

            Order order = new Order();
            order.setRemark("测试订单");

            List<OrderItem> items = new ArrayList<>();
            OrderItem item = new OrderItem();
            item.setProductId(TEST_PRODUCT_ID);
            item.setProductName("测试商品");
            item.setQuantity(2);
            item.setPrice(new BigDecimal("99.99"));
            items.add(item);

            Order createdOrder = orderService.createOrder(order, items, null, TEST_ADDRESS_ID);

            assertNotNull(createdOrder.getId());
            assertEquals(TEST_USER_ID, createdOrder.getUserId());
            assertEquals(0, new BigDecimal("199.98").compareTo(createdOrder.getTotalAmount()));
            assertEquals(0, createdOrder.getStatus());
            assertEquals("测试订单", createdOrder.getRemark());
            assertNotNull(createdOrder.getReceiverName());
            assertNotNull(createdOrder.getReceiverPhone());
            assertNotNull(createdOrder.getReceiverAddress());

            List<OrderItem> savedItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, createdOrder.getId())
            );
            assertEquals(1, savedItems.size());
            assertEquals(TEST_PRODUCT_ID, savedItems.get(0).getProductId());
            assertEquals(2, savedItems.get(0).getQuantity());

            List<StockReservation> reservations = stockReservationMapper.selectList(
                new LambdaQueryWrapper<StockReservation>().eq(StockReservation::getOrderId, createdOrder.getId())
            );
            assertEquals(1, reservations.size());
            assertEquals(StockReservationStatusEnum.RESERVED.getCode(), reservations.get(0).getStatus());
            assertEquals(2, reservations.get(0).getQuantity());

            Product productAfter = productMapper.selectById(TEST_PRODUCT_ID);
            assertEquals(2, productAfter.getReservedStock());
        }
    }

    @Test
    @DisplayName("创建订单 - 使用优惠券")
    void testCreateOrder_WithCoupon() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("ADMIN");

            ensureTestDataExists();

            CouponTemplate template = new CouponTemplate();
            template.setName("测试满减券");
            template.setType(1);
            template.setDiscountAmount(new BigDecimal("20"));
            template.setMinAmount(new BigDecimal("100"));
            template.setTotalCount(100);
            template.setReceivedCount(0);
            template.setUsedCount(0);
            template.setPerUserLimit(2);
            template.setValidStartTime(java.time.LocalDateTime.now().minusDays(1));
            template.setValidEndTime(java.time.LocalDateTime.now().plusDays(30));
            template.setStatus(1);
            couponTemplateMapper.insert(template);

            UserCoupon userCoupon = new UserCoupon();
            userCoupon.setUserId(TEST_USER_ID);
            userCoupon.setTemplateId(template.getId());
            userCoupon.setCouponCode("CPTEST001");
            userCoupon.setStatus(0);
            userCoupon.setValidStartTime(java.time.LocalDateTime.now().minusDays(1));
            userCoupon.setValidEndTime(java.time.LocalDateTime.now().plusDays(30));
            userCouponMapper.insert(userCoupon);

            mocked.when(SecurityUtil::isAdmin).thenReturn(false);

            Order order = new Order();
            List<OrderItem> items = new ArrayList<>();
            OrderItem item = new OrderItem();
            item.setProductId(TEST_PRODUCT_ID);
            item.setProductName("测试商品");
            item.setQuantity(2);
            item.setPrice(new BigDecimal("99.99"));
            items.add(item);

            Order createdOrder = orderService.createOrder(order, items, userCoupon.getId(), TEST_ADDRESS_ID);

            assertNotNull(createdOrder.getId());
            assertEquals(userCoupon.getId(), createdOrder.getCouponId());
            assertEquals(0, new BigDecimal("20").compareTo(createdOrder.getDiscountAmount()));
            assertEquals(0, new BigDecimal("179.98").compareTo(createdOrder.getTotalAmount()));

            UserCoupon usedCoupon = userCouponMapper.selectById(userCoupon.getId());
            assertEquals(1, usedCoupon.getStatus());
            assertEquals(createdOrder.getId(), usedCoupon.getOrderId());
        }
    }

    @Test
    @DisplayName("创建订单 - 收货地址不存在失败")
    void testCreateOrder_AddressNotFound() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            Order order = new Order();
            List<OrderItem> items = new ArrayList<>();
            OrderItem item = new OrderItem();
            item.setProductId(TEST_PRODUCT_ID);
            item.setProductName("测试商品");
            item.setQuantity(1);
            item.setPrice(new BigDecimal("99.99"));
            items.add(item);

            assertThrows(IllegalArgumentException.class, () ->
                orderService.createOrder(order, items, null, 9999L));
        }
    }

    @Test
    @DisplayName("创建订单 - 无收货地址失败")
    void testCreateOrder_NoAddress() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            Long newUserId = 999L;
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(newUserId);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser2");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            Order order = new Order();
            List<OrderItem> items = new ArrayList<>();
            OrderItem item = new OrderItem();
            item.setProductId(TEST_PRODUCT_ID);
            item.setProductName("测试商品");
            item.setQuantity(1);
            item.setPrice(new BigDecimal("99.99"));
            items.add(item);

            assertThrows(IllegalArgumentException.class, () ->
                orderService.createOrder(order, items, null, null));
        }
    }

    @Test
    @DisplayName("创建订单 - 商品库存不足失败")
    void testCreateOrder_InsufficientStock() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            ensureTestDataExists();

            Order order = new Order();
            List<OrderItem> items = new ArrayList<>();
            OrderItem item = new OrderItem();
            item.setProductId(TEST_PRODUCT_ID);
            item.setProductName("测试商品");
            item.setQuantity(99999);
            item.setPrice(new BigDecimal("99.99"));
            items.add(item);

            assertThrows(IllegalArgumentException.class, () ->
                orderService.createOrder(order, items, null, TEST_ADDRESS_ID));
        }
    }

    @Test
    @DisplayName("更新订单状态 - 正常流程")
    void testUpdateOrderStatus_Success() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            ensureTestDataExists();

            Order order = new Order();
            order.setUserId(TEST_USER_ID);
            order.setTotalAmount(new BigDecimal("99.99"));
            order.setStatus(0);
            order.setVersion(1);
            orderMapper.insert(order);

            orderService.updateOrderStatus(order.getId(), 1, 1);

            Order updatedOrder = orderMapper.selectById(order.getId());
            assertEquals(1, updatedOrder.getStatus());
            assertEquals(2, updatedOrder.getVersion());
        }
    }

    @Test
    @DisplayName("更新订单状态 - 乐观锁冲突失败")
    void testUpdateOrderStatus_OptimisticLockConflict() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            ensureTestDataExists();

            Order order = new Order();
            order.setUserId(TEST_USER_ID);
            order.setTotalAmount(new BigDecimal("99.99"));
            order.setStatus(0);
            order.setVersion(1);
            orderMapper.insert(order);

            orderService.updateOrderStatus(order.getId(), 1, 1);

            assertThrows(IllegalArgumentException.class, () ->
                orderService.updateOrderStatus(order.getId(), 2, 1));
        }
    }

    @Test
    @DisplayName("更新订单状态 - 订单不存在失败")
    void testUpdateOrderStatus_OrderNotFound() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            assertThrows(IllegalArgumentException.class, () ->
                orderService.updateOrderStatus(99999L, 1, 1));
        }
    }

    @Test
    @DisplayName("更新订单状态 - 无权修改他人订单失败")
    void testUpdateOrderStatus_NoPermission() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            Order order = new Order();
            order.setUserId(999L);
            order.setTotalAmount(new BigDecimal("99.99"));
            order.setStatus(0);
            order.setVersion(1);
            orderMapper.insert(order);

            assertThrows(SecurityException.class, () ->
                orderService.updateOrderStatus(order.getId(), 1, 1));
        }
    }

    @Test
    @DisplayName("更新订单状态 - 取消订单恢复优惠券和库存")
    void testUpdateOrderStatus_CancelOrderRestoreCouponAndStock() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("ADMIN");

            ensureTestDataExists();

            CouponTemplate template = new CouponTemplate();
            template.setName("测试满减券");
            template.setType(1);
            template.setDiscountAmount(new BigDecimal("10"));
            template.setMinAmount(new BigDecimal("50"));
            template.setTotalCount(100);
            template.setReceivedCount(1);
            template.setUsedCount(1);
            template.setPerUserLimit(2);
            template.setValidStartTime(java.time.LocalDateTime.now().minusDays(1));
            template.setValidEndTime(java.time.LocalDateTime.now().plusDays(30));
            template.setStatus(1);
            couponTemplateMapper.insert(template);

            UserCoupon userCoupon = new UserCoupon();
            userCoupon.setUserId(TEST_USER_ID);
            userCoupon.setTemplateId(template.getId());
            userCoupon.setCouponCode("CPTEST002");
            userCoupon.setStatus(1);
            userCoupon.setOrderId(999L);
            userCoupon.setDiscountAmount(new BigDecimal("10"));
            userCoupon.setUsedTime(java.time.LocalDateTime.now());
            userCoupon.setValidStartTime(java.time.LocalDateTime.now().minusDays(1));
            userCoupon.setValidEndTime(java.time.LocalDateTime.now().plusDays(30));
            userCouponMapper.insert(userCoupon);

            Order order = new Order();
            order.setUserId(TEST_USER_ID);
            order.setTotalAmount(new BigDecimal("89.99"));
            order.setStatus(1);
            order.setVersion(1);
            order.setCouponId(userCoupon.getId());
            order.setDiscountAmount(new BigDecimal("10"));
            orderMapper.insert(order);

            StockReservation reservation = new StockReservation();
            reservation.setOrderId(order.getId());
            reservation.setProductId(TEST_PRODUCT_ID);
            reservation.setProductName("测试商品");
            reservation.setQuantity(1);
            reservation.setStatus(StockReservationStatusEnum.RESERVED.getCode());
            reservation.setExpireTime(java.time.LocalDateTime.now().plusDays(1));
            stockReservationMapper.insert(reservation);

            Product product = productMapper.selectById(TEST_PRODUCT_ID);
            product.setReservedStock(1);
            productMapper.updateById(product);

            orderService.updateOrderStatus(order.getId(), 4, 1);

            UserCoupon restoredCoupon = userCouponMapper.selectById(userCoupon.getId());
            assertEquals(0, restoredCoupon.getStatus());
            assertNull(restoredCoupon.getOrderId());
            assertNull(restoredCoupon.getUsedTime());

            CouponTemplate updatedTemplate = couponTemplateMapper.selectById(template.getId());
            assertEquals(0, updatedTemplate.getUsedCount());

            StockReservation updatedReservation = stockReservationMapper.selectById(reservation.getId());
            assertEquals(StockReservationStatusEnum.RELEASED.getCode(), updatedReservation.getStatus());

            Product updatedProduct = productMapper.selectById(TEST_PRODUCT_ID);
            assertEquals(0, updatedProduct.getReservedStock());
        }
    }

    @Test
    @DisplayName("更新订单状态 - 订单完成扣减库存")
    void testUpdateOrderStatus_CompleteOrderDeductStock() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            ensureTestDataExists();

            Product productBefore = productMapper.selectById(TEST_PRODUCT_ID);
            int initialStock = productBefore.getStock();

            Order order = new Order();
            order.setUserId(TEST_USER_ID);
            order.setTotalAmount(new BigDecimal("99.99"));
            order.setStatus(2);
            order.setVersion(1);
            orderMapper.insert(order);

            StockReservation reservation = new StockReservation();
            reservation.setOrderId(order.getId());
            reservation.setProductId(TEST_PRODUCT_ID);
            reservation.setProductName("测试商品");
            reservation.setQuantity(2);
            reservation.setStatus(StockReservationStatusEnum.RESERVED.getCode());
            reservation.setExpireTime(java.time.LocalDateTime.now().plusDays(1));
            stockReservationMapper.insert(reservation);

            Product product = productMapper.selectById(TEST_PRODUCT_ID);
            product.setReservedStock(2);
            productMapper.updateById(product);

            orderService.updateOrderStatus(order.getId(), 3, 1);

            StockReservation updatedReservation = stockReservationMapper.selectById(reservation.getId());
            assertEquals(StockReservationStatusEnum.DEDUCTED.getCode(), updatedReservation.getStatus());

            Product productAfter = productMapper.selectById(TEST_PRODUCT_ID);
            assertEquals(initialStock - 2, productAfter.getStock());
            assertEquals(0, productAfter.getReservedStock());
        }
    }

    @Test
    @DisplayName("更新订单备注 - 正常流程")
    void testUpdateRemark_Success() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            Order order = new Order();
            order.setUserId(TEST_USER_ID);
            order.setTotalAmount(new BigDecimal("99.99"));
            order.setStatus(0);
            order.setVersion(1);
            order.setRemark("初始备注");
            orderMapper.insert(order);

            orderService.updateRemark(order.getId(), "更新后的备注", 1);

            Order updatedOrder = orderMapper.selectById(order.getId());
            assertEquals("更新后的备注", updatedOrder.getRemark());
            assertEquals(2, updatedOrder.getVersion());
        }
    }

    @Test
    @DisplayName("更新订单备注 - 乐观锁冲突失败")
    void testUpdateRemark_OptimisticLockConflict() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            Order order = new Order();
            order.setUserId(TEST_USER_ID);
            order.setTotalAmount(new BigDecimal("99.99"));
            order.setStatus(0);
            order.setVersion(1);
            orderMapper.insert(order);

            orderService.updateRemark(order.getId(), "第一次更新", 1);

            assertThrows(IllegalArgumentException.class, () ->
                orderService.updateRemark(order.getId(), "第二次更新", 1));
        }
    }

    @Test
    @DisplayName("获取订单详情 - 正常流程")
    void testGetOrderDetail_Success() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            ensureTestDataExists();

            Order order = new Order();
            order.setUserId(TEST_USER_ID);
            order.setTotalAmount(new BigDecimal("199.98"));
            order.setStatus(0);
            order.setVersion(1);
            order.setAddressId(TEST_ADDRESS_ID);
            orderMapper.insert(order);

            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setProductId(TEST_PRODUCT_ID);
            item.setProductName("测试商品");
            item.setQuantity(2);
            item.setPrice(new BigDecimal("99.99"));
            orderItemMapper.insert(item);

            var detail = orderService.getOrderDetail(order.getId());

            assertNotNull(detail);
            assertEquals(order.getId(), detail.getId());
            assertEquals(0, new BigDecimal("199.98").compareTo(detail.getTotalAmount()));
        }
    }

    @Test
    @DisplayName("获取订单详情 - 无权查看他人订单失败")
    void testGetOrderDetail_NoPermission() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            Order order = new Order();
            order.setUserId(999L);
            order.setTotalAmount(new BigDecimal("99.99"));
            order.setStatus(0);
            order.setVersion(1);
            orderMapper.insert(order);

            assertThrows(SecurityException.class, () ->
                orderService.getOrderDetail(order.getId()));
        }
    }

    @Test
    @DisplayName("获取订单详情 - 订单不存在失败")
    void testGetOrderDetail_NotFound() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            assertThrows(IllegalArgumentException.class, () ->
                orderService.getOrderDetail(99999L));
        }
    }

    @Test
    @DisplayName("处理退款 - 管理员权限成功")
    void testProcessRefund_AdminSuccess() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("admin");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("ADMIN");

            Order order = new Order();
            order.setUserId(TEST_USER_ID);
            order.setTotalAmount(new BigDecimal("99.99"));
            order.setStatus(5);
            order.setVersion(1);
            orderMapper.insert(order);

            assertDoesNotThrow(() ->
                orderService.processRefund(order.getId(), true, "退款成功"));
        }
    }

    @Test
    @DisplayName("处理退款 - 非管理员权限失败")
    void testProcessRefund_NonAdminFailure() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            Order order = new Order();
            order.setUserId(TEST_USER_ID);
            order.setTotalAmount(new BigDecimal("99.99"));
            order.setStatus(5);
            order.setVersion(1);
            orderMapper.insert(order);

            assertThrows(SecurityException.class, () ->
                orderService.processRefund(order.getId(), true, "退款成功"));
        }
    }

    private void ensureTestDataExists() {
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

        UserAddress address = userAddressMapper.selectById(TEST_ADDRESS_ID);
        if (address == null) {
            address = new UserAddress();
            address.setId(TEST_ADDRESS_ID);
            address.setUserId(TEST_USER_ID);
            address.setReceiverName("张三");
            address.setReceiverPhone("13800138000");
            address.setProvince("广东省");
            address.setCity("深圳市");
            address.setDistrict("南山区");
            address.setDetailAddress("科技园路1号");
            address.setIsDefault(1);
            address.setDeleted(0);
            userAddressMapper.insert(address);
        }
    }
}
