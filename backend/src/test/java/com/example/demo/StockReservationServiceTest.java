package com.example.demo;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.dto.StockReservationCreateDTO;
import com.example.demo.entity.Product;
import com.example.demo.entity.StockReservation;
import com.example.demo.enums.StockReservationStatusEnum;
import com.example.demo.mapper.ProductMapper;
import com.example.demo.mapper.StockReservationMapper;
import com.example.demo.service.StockReservationService;
import com.example.demo.util.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class StockReservationServiceTest {

    @Autowired
    private StockReservationService stockReservationService;

    @Autowired
    private StockReservationMapper stockReservationMapper;

    @Autowired
    private ProductMapper productMapper;

    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_PRODUCT_ID_1 = 1L;
    private static final Long TEST_PRODUCT_ID_2 = 2L;
    private static final Long TEST_ORDER_ID = 100L;

    @BeforeEach
    void setUp() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");
        }
        ensureTestProductsExist();
    }

    @Test
    @DisplayName("创建库存预占 - 单商品正常流程")
    void testCreateReservations_SingleProduct() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            Product productBefore = productMapper.selectById(TEST_PRODUCT_ID_1);
            int initialReserved = productBefore.getReservedStock() == null ? 0 : productBefore.getReservedStock();

            StockReservationCreateDTO dto = new StockReservationCreateDTO();
            dto.setOrderId(TEST_ORDER_ID);
            dto.setExpireTime(LocalDateTime.now().plusMinutes(30));

            List<StockReservationCreateDTO.ReservationItem> items = new ArrayList<>();
            StockReservationCreateDTO.ReservationItem item = new StockReservationCreateDTO.ReservationItem();
            item.setOrderItemId(1L);
            item.setProductId(TEST_PRODUCT_ID_1);
            item.setProductName("测试商品1");
            item.setQuantity(3);
            items.add(item);
            dto.setItems(items);

            stockReservationService.createReservations(dto);

            List<StockReservation> reservations = stockReservationMapper.selectList(
                new LambdaQueryWrapper<StockReservation>().eq(StockReservation::getOrderId, TEST_ORDER_ID)
            );
            assertEquals(1, reservations.size());
            assertEquals(TEST_PRODUCT_ID_1, reservations.get(0).getProductId());
            assertEquals(3, reservations.get(0).getQuantity());
            assertEquals(StockReservationStatusEnum.RESERVED.getCode(), reservations.get(0).getStatus());

            Product productAfter = productMapper.selectById(TEST_PRODUCT_ID_1);
            assertEquals(initialReserved + 3, productAfter.getReservedStock());
        }
    }

    @Test
    @DisplayName("创建库存预占 - 多商品正常流程")
    void testCreateReservations_MultipleProducts() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            Product product1Before = productMapper.selectById(TEST_PRODUCT_ID_1);
            Product product2Before = productMapper.selectById(TEST_PRODUCT_ID_2);
            int initialReserved1 = product1Before.getReservedStock() == null ? 0 : product1Before.getReservedStock();
            int initialReserved2 = product2Before.getReservedStock() == null ? 0 : product2Before.getReservedStock();

            StockReservationCreateDTO dto = new StockReservationCreateDTO();
            dto.setOrderId(TEST_ORDER_ID + 1);
            dto.setExpireTime(LocalDateTime.now().plusMinutes(30));

            List<StockReservationCreateDTO.ReservationItem> items = new ArrayList<>();

            StockReservationCreateDTO.ReservationItem item1 = new StockReservationCreateDTO.ReservationItem();
            item1.setOrderItemId(1L);
            item1.setProductId(TEST_PRODUCT_ID_1);
            item1.setProductName("测试商品1");
            item1.setQuantity(2);
            items.add(item1);

            StockReservationCreateDTO.ReservationItem item2 = new StockReservationCreateDTO.ReservationItem();
            item2.setOrderItemId(2L);
            item2.setProductId(TEST_PRODUCT_ID_2);
            item2.setProductName("测试商品2");
            item2.setQuantity(5);
            items.add(item2);

            dto.setItems(items);

            stockReservationService.createReservations(dto);

            List<StockReservation> reservations = stockReservationMapper.selectList(
                new LambdaQueryWrapper<StockReservation>().eq(StockReservation::getOrderId, TEST_ORDER_ID + 1)
            );
            assertEquals(2, reservations.size());

            Product product1After = productMapper.selectById(TEST_PRODUCT_ID_1);
            Product product2After = productMapper.selectById(TEST_PRODUCT_ID_2);
            assertEquals(initialReserved1 + 2, product1After.getReservedStock());
            assertEquals(initialReserved2 + 5, product2After.getReservedStock());
        }
    }

    @Test
    @DisplayName("创建库存预占 - 商品不存在失败")
    void testCreateReservations_ProductNotFound() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            StockReservationCreateDTO dto = new StockReservationCreateDTO();
            dto.setOrderId(TEST_ORDER_ID);
            dto.setExpireTime(LocalDateTime.now().plusMinutes(30));

            List<StockReservationCreateDTO.ReservationItem> items = new ArrayList<>();
            StockReservationCreateDTO.ReservationItem item = new StockReservationCreateDTO.ReservationItem();
            item.setOrderItemId(1L);
            item.setProductId(99999L);
            item.setProductName("不存在的商品");
            item.setQuantity(1);
            items.add(item);
            dto.setItems(items);

            assertThrows(IllegalArgumentException.class, () ->
                stockReservationService.createReservations(dto));
        }
    }

    @Test
    @DisplayName("创建库存预占 - 库存不足失败")
    void testCreateReservations_InsufficientStock() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            StockReservationCreateDTO dto = new StockReservationCreateDTO();
            dto.setOrderId(TEST_ORDER_ID);
            dto.setExpireTime(LocalDateTime.now().plusMinutes(30));

            List<StockReservationCreateDTO.ReservationItem> items = new ArrayList<>();
            StockReservationCreateDTO.ReservationItem item = new StockReservationCreateDTO.ReservationItem();
            item.setOrderItemId(1L);
            item.setProductId(TEST_PRODUCT_ID_1);
            item.setProductName("测试商品1");
            item.setQuantity(99999);
            items.add(item);
            dto.setItems(items);

            assertThrows(IllegalArgumentException.class, () ->
                stockReservationService.createReservations(dto));
        }
    }

    @Test
    @DisplayName("创建库存预占 - 事务回滚测试：部分商品库存不足时全部回滚")
    void testCreateReservations_TransactionRollback() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            Product product1Before = productMapper.selectById(TEST_PRODUCT_ID_1);
            Product product2Before = productMapper.selectById(TEST_PRODUCT_ID_2);
            int initialReserved1 = product1Before.getReservedStock() == null ? 0 : product1Before.getReservedStock();
            int initialReserved2 = product2Before.getReservedStock() == null ? 0 : product2Before.getReservedStock();

            StockReservationCreateDTO dto = new StockReservationCreateDTO();
            dto.setOrderId(TEST_ORDER_ID + 10);
            dto.setExpireTime(LocalDateTime.now().plusMinutes(30));

            List<StockReservationCreateDTO.ReservationItem> items = new ArrayList<>();

            StockReservationCreateDTO.ReservationItem item1 = new StockReservationCreateDTO.ReservationItem();
            item1.setOrderItemId(1L);
            item1.setProductId(TEST_PRODUCT_ID_1);
            item1.setProductName("测试商品1");
            item1.setQuantity(2);
            items.add(item1);

            StockReservationCreateDTO.ReservationItem item2 = new StockReservationCreateDTO.ReservationItem();
            item2.setOrderItemId(2L);
            item2.setProductId(TEST_PRODUCT_ID_2);
            item2.setProductName("测试商品2");
            item2.setQuantity(99999);
            items.add(item2);

            dto.setItems(items);

            assertThrows(IllegalArgumentException.class, () ->
                stockReservationService.createReservations(dto));

            List<StockReservation> reservations = stockReservationMapper.selectList(
                new LambdaQueryWrapper<StockReservation>().eq(StockReservation::getOrderId, TEST_ORDER_ID + 10)
            );
            assertEquals(0, reservations.size());

            Product product1After = productMapper.selectById(TEST_PRODUCT_ID_1);
            Product product2After = productMapper.selectById(TEST_PRODUCT_ID_2);
            assertEquals(initialReserved1, product1After.getReservedStock());
            assertEquals(initialReserved2, product2After.getReservedStock());
        }
    }

    @Test
    @DisplayName("释放库存预占 - 正常流程")
    void testReleaseReservations_Success() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            Product productBefore = productMapper.selectById(TEST_PRODUCT_ID_1);
            productBefore.setReservedStock(5);
            productMapper.updateById(productBefore);

            Long orderId = TEST_ORDER_ID + 2;
            StockReservation reservation = new StockReservation();
            reservation.setOrderId(orderId);
            reservation.setOrderItemId(1L);
            reservation.setProductId(TEST_PRODUCT_ID_1);
            reservation.setProductName("测试商品1");
            reservation.setQuantity(3);
            reservation.setStatus(StockReservationStatusEnum.RESERVED.getCode());
            reservation.setExpireTime(LocalDateTime.now().plusDays(1));
            stockReservationMapper.insert(reservation);

            stockReservationService.releaseReservations(orderId, "测试释放");

            StockReservation updatedReservation = stockReservationMapper.selectById(reservation.getId());
            assertEquals(StockReservationStatusEnum.RELEASED.getCode(), updatedReservation.getStatus());
            assertEquals("测试释放", updatedReservation.getReleaseReason());

            Product productAfter = productMapper.selectById(TEST_PRODUCT_ID_1);
            assertEquals(2, productAfter.getReservedStock());
        }
    }

    @Test
    @DisplayName("释放库存预占 - 无预占记录不报错")
    void testReleaseReservations_NoRecords() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            assertDoesNotThrow(() ->
                stockReservationService.releaseReservations(99999L, "测试释放"));
        }
    }

    @Test
    @DisplayName("释放库存预占 - 只释放预占中的记录")
    void testReleaseReservations_OnlyReservedStatus() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            Product productBefore = productMapper.selectById(TEST_PRODUCT_ID_1);
            productBefore.setReservedStock(5);
            productMapper.updateById(productBefore);

            Long orderId = TEST_ORDER_ID + 3;

            StockReservation reserved = new StockReservation();
            reserved.setOrderId(orderId);
            reserved.setOrderItemId(1L);
            reserved.setProductId(TEST_PRODUCT_ID_1);
            reserved.setProductName("测试商品1");
            reserved.setQuantity(2);
            reserved.setStatus(StockReservationStatusEnum.RESERVED.getCode());
            reserved.setExpireTime(LocalDateTime.now().plusDays(1));
            stockReservationMapper.insert(reserved);

            StockReservation alreadyReleased = new StockReservation();
            alreadyReleased.setOrderId(orderId);
            alreadyReleased.setOrderItemId(2L);
            alreadyReleased.setProductId(TEST_PRODUCT_ID_1);
            alreadyReleased.setProductName("测试商品1");
            alreadyReleased.setQuantity(3);
            alreadyReleased.setStatus(StockReservationStatusEnum.RELEASED.getCode());
            alreadyReleased.setExpireTime(LocalDateTime.now().plusDays(1));
            stockReservationMapper.insert(alreadyReleased);

            stockReservationService.releaseReservations(orderId, "测试释放");

            Product productAfter = productMapper.selectById(TEST_PRODUCT_ID_1);
            assertEquals(3, productAfter.getReservedStock());
        }
    }

    @Test
    @DisplayName("正式扣减库存 - 正常流程")
    void testDeductStock_Success() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            Product productBefore = productMapper.selectById(TEST_PRODUCT_ID_1);
            int initialStock = productBefore.getStock();
            productBefore.setReservedStock(4);
            productMapper.updateById(productBefore);

            Long orderId = TEST_ORDER_ID + 4;
            StockReservation reservation = new StockReservation();
            reservation.setOrderId(orderId);
            reservation.setOrderItemId(1L);
            reservation.setProductId(TEST_PRODUCT_ID_1);
            reservation.setProductName("测试商品1");
            reservation.setQuantity(3);
            reservation.setStatus(StockReservationStatusEnum.RESERVED.getCode());
            reservation.setExpireTime(LocalDateTime.now().plusDays(1));
            stockReservationMapper.insert(reservation);

            stockReservationService.deductStock(orderId);

            StockReservation updatedReservation = stockReservationMapper.selectById(reservation.getId());
            assertEquals(StockReservationStatusEnum.DEDUCTED.getCode(), updatedReservation.getStatus());

            Product productAfter = productMapper.selectById(TEST_PRODUCT_ID_1);
            assertEquals(initialStock - 3, productAfter.getStock());
            assertEquals(1, productAfter.getReservedStock());
        }
    }

    @Test
    @DisplayName("正式扣减库存 - 无预占记录不报错")
    void testDeductStock_NoRecords() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            assertDoesNotThrow(() ->
                stockReservationService.deductStock(99999L));
        }
    }

    @Test
    @DisplayName("释放过期预占 - 正常流程")
    void testReleaseExpiredReservations_Success() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            Product productBefore = productMapper.selectById(TEST_PRODUCT_ID_1);
            productBefore.setReservedStock(3);
            productMapper.updateById(productBefore);

            Long orderId = TEST_ORDER_ID + 5;
            StockReservation expiredReservation = new StockReservation();
            expiredReservation.setOrderId(orderId);
            expiredReservation.setOrderItemId(1L);
            expiredReservation.setProductId(TEST_PRODUCT_ID_1);
            expiredReservation.setProductName("测试商品1");
            expiredReservation.setQuantity(3);
            expiredReservation.setStatus(StockReservationStatusEnum.RESERVED.getCode());
            expiredReservation.setExpireTime(LocalDateTime.now().minusMinutes(1));
            stockReservationMapper.insert(expiredReservation);

            int releasedCount = stockReservationService.releaseExpiredReservations();

            assertTrue(releasedCount >= 1);

            StockReservation updatedReservation = stockReservationMapper.selectById(expiredReservation.getId());
            assertEquals(StockReservationStatusEnum.RELEASED.getCode(), updatedReservation.getStatus());

            Product productAfter = productMapper.selectById(TEST_PRODUCT_ID_1);
            assertEquals(0, productAfter.getReservedStock());
        }
    }

    @Test
    @DisplayName("释放过期预占 - 未过期的不释放")
    void testReleaseExpiredReservations_NotExpired() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            Product productBefore = productMapper.selectById(TEST_PRODUCT_ID_1);
            productBefore.setReservedStock(5);
            productMapper.updateById(productBefore);

            Long orderId = TEST_ORDER_ID + 6;

            StockReservation expired = new StockReservation();
            expired.setOrderId(orderId);
            expired.setOrderItemId(1L);
            expired.setProductId(TEST_PRODUCT_ID_1);
            expired.setProductName("测试商品1");
            expired.setQuantity(2);
            expired.setStatus(StockReservationStatusEnum.RESERVED.getCode());
            expired.setExpireTime(LocalDateTime.now().minusMinutes(1));
            stockReservationMapper.insert(expired);

            StockReservation notExpired = new StockReservation();
            notExpired.setOrderId(orderId + 1);
            notExpired.setOrderItemId(2L);
            notExpired.setProductId(TEST_PRODUCT_ID_1);
            notExpired.setProductName("测试商品1");
            notExpired.setQuantity(3);
            notExpired.setStatus(StockReservationStatusEnum.RESERVED.getCode());
            notExpired.setExpireTime(LocalDateTime.now().plusMinutes(30));
            stockReservationMapper.insert(notExpired);

            stockReservationService.releaseExpiredReservations();

            StockReservation updatedExpired = stockReservationMapper.selectById(expired.getId());
            assertEquals(StockReservationStatusEnum.RELEASED.getCode(), updatedExpired.getStatus());

            StockReservation updatedNotExpired = stockReservationMapper.selectById(notExpired.getId());
            assertEquals(StockReservationStatusEnum.RESERVED.getCode(), updatedNotExpired.getStatus());

            Product productAfter = productMapper.selectById(TEST_PRODUCT_ID_1);
            assertEquals(3, productAfter.getReservedStock());
        }
    }

    @Test
    @DisplayName("释放过期预占 - 无过期记录返回0")
    void testReleaseExpiredReservations_NoExpired() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("admin");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("ADMIN");

            stockReservationMapper.delete(new LambdaQueryWrapper<StockReservation>());

            Product product = productMapper.selectById(TEST_PRODUCT_ID_1);
            product.setReservedStock(0);
            productMapper.updateById(product);

            int count = stockReservationService.releaseExpiredReservations();
            assertEquals(0, count);
        }
    }

    @Test
    @DisplayName("根据订单ID查询预占记录")
    void testGetReservationsByOrderId() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            Long orderId = TEST_ORDER_ID + 7;

            StockReservation reservation1 = new StockReservation();
            reservation1.setOrderId(orderId);
            reservation1.setOrderItemId(1L);
            reservation1.setProductId(TEST_PRODUCT_ID_1);
            reservation1.setProductName("测试商品1");
            reservation1.setQuantity(2);
            reservation1.setStatus(StockReservationStatusEnum.RESERVED.getCode());
            reservation1.setExpireTime(LocalDateTime.now().plusDays(1));
            stockReservationMapper.insert(reservation1);

            StockReservation reservation2 = new StockReservation();
            reservation2.setOrderId(orderId);
            reservation2.setOrderItemId(2L);
            reservation2.setProductId(TEST_PRODUCT_ID_2);
            reservation2.setProductName("测试商品2");
            reservation2.setQuantity(3);
            reservation2.setStatus(StockReservationStatusEnum.RESERVED.getCode());
            reservation2.setExpireTime(LocalDateTime.now().plusDays(1));
            stockReservationMapper.insert(reservation2);

            var result = stockReservationService.getReservationsByOrderId(orderId);

            assertNotNull(result);
            assertEquals(2, result.size());
        }
    }

    @Test
    @DisplayName("分页查询预占记录 - 普通用户权限")
    void testPageReservations_UserPermission() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            var page = stockReservationService.pageReservations(1, 10, null, null, null);

            assertNotNull(page);
            assertTrue(page.getRecords().size() <= 10);
        }
    }

    @Test
    @DisplayName("分页查询预占记录 - 管理员权限")
    void testPageReservations_AdminPermission() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("admin");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("ADMIN");

            var page = stockReservationService.pageReservations(1, 10, null, null, null);

            assertNotNull(page);
            assertTrue(page.getRecords().size() <= 10);
        }
    }

    @Test
    @DisplayName("并发创建库存预占 - 库存数量校验")
    void testCreateReservations_ConcurrentStockCheck() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            Product product = productMapper.selectById(TEST_PRODUCT_ID_1);
            product.setStock(5);
            product.setReservedStock(0);
            productMapper.updateById(product);

            StockReservationCreateDTO dto1 = new StockReservationCreateDTO();
            dto1.setOrderId(TEST_ORDER_ID + 20);
            dto1.setExpireTime(LocalDateTime.now().plusMinutes(30));
            List<StockReservationCreateDTO.ReservationItem> items1 = new ArrayList<>();
            StockReservationCreateDTO.ReservationItem item1 = new StockReservationCreateDTO.ReservationItem();
            item1.setOrderItemId(1L);
            item1.setProductId(TEST_PRODUCT_ID_1);
            item1.setProductName("测试商品1");
            item1.setQuantity(3);
            items1.add(item1);
            dto1.setItems(items1);

            stockReservationService.createReservations(dto1);

            Product productAfter1 = productMapper.selectById(TEST_PRODUCT_ID_1);
            assertEquals(3, productAfter1.getReservedStock());

            StockReservationCreateDTO dto2 = new StockReservationCreateDTO();
            dto2.setOrderId(TEST_ORDER_ID + 21);
            dto2.setExpireTime(LocalDateTime.now().plusMinutes(30));
            List<StockReservationCreateDTO.ReservationItem> items2 = new ArrayList<>();
            StockReservationCreateDTO.ReservationItem item2 = new StockReservationCreateDTO.ReservationItem();
            item2.setOrderItemId(1L);
            item2.setProductId(TEST_PRODUCT_ID_1);
            item2.setProductName("测试商品1");
            item2.setQuantity(3);
            items2.add(item2);
            dto2.setItems(items2);

            assertThrows(IllegalArgumentException.class, () ->
                stockReservationService.createReservations(dto2));

            Product productAfter2 = productMapper.selectById(TEST_PRODUCT_ID_1);
            assertEquals(3, productAfter2.getReservedStock());
        }
    }

    private void ensureTestProductsExist() {
        Product product1 = productMapper.selectById(TEST_PRODUCT_ID_1);
        if (product1 == null) {
            product1 = new Product();
            product1.setId(TEST_PRODUCT_ID_1);
            product1.setName("测试商品1");
            product1.setPrice(java.math.BigDecimal.valueOf(99.99));
            product1.setStock(100);
            product1.setReservedStock(0);
            product1.setCategory("测试分类");
            productMapper.insert(product1);
        } else {
            product1.setStock(100);
            product1.setReservedStock(0);
            productMapper.updateById(product1);
        }

        Product product2 = productMapper.selectById(TEST_PRODUCT_ID_2);
        if (product2 == null) {
            product2 = new Product();
            product2.setId(TEST_PRODUCT_ID_2);
            product2.setName("测试商品2");
            product2.setPrice(java.math.BigDecimal.valueOf(49.99));
            product2.setStock(200);
            product2.setReservedStock(0);
            product2.setCategory("测试分类");
            productMapper.insert(product2);
        } else {
            product2.setStock(200);
            product2.setReservedStock(0);
            productMapper.updateById(product2);
        }
    }
}
