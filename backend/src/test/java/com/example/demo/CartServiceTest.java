package com.example.demo;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.entity.*;
import com.example.demo.mapper.*;
import com.example.demo.service.CartService;
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
class CartServiceTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private UserAddressMapper userAddressMapper;

    @Autowired
    private CouponTemplateMapper couponTemplateMapper;

    @Autowired
    private UserCouponMapper userCouponMapper;

    @Autowired
    private StockReservationMapper stockReservationMapper;

    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_PRODUCT_ID_1 = 1L;
    private static final Long TEST_PRODUCT_ID_2 = 2L;
    private static final Long TEST_ADDRESS_ID = 1L;

    @BeforeEach
    void setUp() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");
        }
        ensureTestDataExists();
        clearUserCart();
    }

    @Test
    @DisplayName("添加商品到购物车 - 新商品成功")
    void testAddToCart_NewProduct() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            Cart cart = cartService.addToCart(TEST_PRODUCT_ID_1, 2);

            assertNotNull(cart.getId());
            assertEquals(TEST_USER_ID, cart.getUserId());
            assertEquals(TEST_PRODUCT_ID_1, cart.getProductId());
            assertEquals(2, cart.getQuantity());

            List<Cart> carts = cartMapper.selectList(
                new LambdaQueryWrapper<Cart>()
                    .eq(Cart::getUserId, TEST_USER_ID)
                    .eq(Cart::getProductId, TEST_PRODUCT_ID_1)
            );
            assertEquals(1, carts.size());
            assertEquals(2, carts.get(0).getQuantity());
        }
    }

    @Test
    @DisplayName("添加商品到购物车 - 已有商品累加数量")
    void testAddToCart_ExistingProduct() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            cartService.addToCart(TEST_PRODUCT_ID_1, 2);
            Cart updatedCart = cartService.addToCart(TEST_PRODUCT_ID_1, 3);

            assertEquals(5, updatedCart.getQuantity());

            List<Cart> carts = cartMapper.selectList(
                new LambdaQueryWrapper<Cart>()
                    .eq(Cart::getUserId, TEST_USER_ID)
                    .eq(Cart::getProductId, TEST_PRODUCT_ID_1)
            );
            assertEquals(1, carts.size());
            assertEquals(5, carts.get(0).getQuantity());
        }
    }

    @Test
    @DisplayName("添加商品到购物车 - 商品不存在失败")
    void testAddToCart_ProductNotFound() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            assertThrows(IllegalArgumentException.class, () ->
                cartService.addToCart(99999L, 1));
        }
    }

    @Test
    @DisplayName("添加商品到购物车 - 库存不足失败")
    void testAddToCart_InsufficientStock() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            assertThrows(IllegalArgumentException.class, () ->
                cartService.addToCart(TEST_PRODUCT_ID_1, 99999));
        }
    }

    @Test
    @DisplayName("添加商品到购物车 - 累加后库存不足失败")
    void testAddToCart_AccumulatedInsufficientStock() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            Product product = productMapper.selectById(TEST_PRODUCT_ID_1);
            product.setStock(10);
            product.setReservedStock(0);
            productMapper.updateById(product);

            cartService.addToCart(TEST_PRODUCT_ID_1, 8);

            assertThrows(IllegalArgumentException.class, () ->
                cartService.addToCart(TEST_PRODUCT_ID_1, 5));
        }
    }

    @Test
    @DisplayName("获取我的购物车 - 正常流程")
    void testGetMyCart_Success() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            cartService.addToCart(TEST_PRODUCT_ID_1, 2);
            cartService.addToCart(TEST_PRODUCT_ID_2, 3);

            var cartItems = cartService.getMyCart();

            assertNotNull(cartItems);
            assertTrue(cartItems.size() >= 2);
        }
    }

    @Test
    @DisplayName("更新购物车数量 - 正常流程")
    void testUpdateQuantity_Success() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            Cart cart = cartService.addToCart(TEST_PRODUCT_ID_1, 2);

            cartService.updateQuantity(cart.getId(), 5);

            Cart updatedCart = cartMapper.selectById(cart.getId());
            assertEquals(5, updatedCart.getQuantity());
        }
    }

    @Test
    @DisplayName("更新购物车数量 - 购物车项不存在失败")
    void testUpdateQuantity_NotFound() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            assertThrows(IllegalArgumentException.class, () ->
                cartService.updateQuantity(99999L, 1));
        }
    }

    @Test
    @DisplayName("更新购物车数量 - 无权操作他人购物车失败")
    void testUpdateQuantity_NoPermission() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(999L);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("otheruser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            Cart otherCart = new Cart();
            otherCart.setUserId(TEST_USER_ID);
            otherCart.setProductId(TEST_PRODUCT_ID_1);
            otherCart.setQuantity(2);
            cartMapper.insert(otherCart);

            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID + 1);

            assertThrows(SecurityException.class, () ->
                cartService.updateQuantity(otherCart.getId(), 5));
        }
    }

    @Test
    @DisplayName("更新购物车数量 - 库存不足失败")
    void testUpdateQuantity_InsufficientStock() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            Cart cart = cartService.addToCart(TEST_PRODUCT_ID_1, 2);

            assertThrows(IllegalArgumentException.class, () ->
                cartService.updateQuantity(cart.getId(), 99999));
        }
    }

    @Test
    @DisplayName("删除购物车项 - 正常流程")
    void testRemoveFromCart_Success() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            Cart cart = cartService.addToCart(TEST_PRODUCT_ID_1, 2);

            cartService.removeFromCart(cart.getId());

            Cart deletedCart = cartMapper.selectById(cart.getId());
            assertNull(deletedCart);
        }
    }

    @Test
    @DisplayName("删除购物车项 - 无权操作他人购物车失败")
    void testRemoveFromCart_NoPermission() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            Cart otherCart = new Cart();
            otherCart.setUserId(999L);
            otherCart.setProductId(TEST_PRODUCT_ID_1);
            otherCart.setQuantity(2);
            cartMapper.insert(otherCart);

            assertThrows(SecurityException.class, () ->
                cartService.removeFromCart(otherCart.getId()));
        }
    }

    @Test
    @DisplayName("批量删除购物车项 - 正常流程")
    void testBatchRemove_Success() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            Cart cart1 = cartService.addToCart(TEST_PRODUCT_ID_1, 2);
            Cart cart2 = cartService.addToCart(TEST_PRODUCT_ID_2, 3);

            List<Long> ids = new ArrayList<>();
            ids.add(cart1.getId());
            ids.add(cart2.getId());

            cartService.batchRemove(ids);

            assertNull(cartMapper.selectById(cart1.getId()));
            assertNull(cartMapper.selectById(cart2.getId()));
        }
    }

    @Test
    @DisplayName("购物车结算 - 正常流程（单商品）")
    void testCheckout_SingleProduct() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            Product productBefore = productMapper.selectById(TEST_PRODUCT_ID_1);
            int initialStock = productBefore.getStock();

            Cart cart = cartService.addToCart(TEST_PRODUCT_ID_1, 2);

            List<Long> cartIds = new ArrayList<>();
            cartIds.add(cart.getId());

            Order order = cartService.checkout(cartIds, "测试订单备注", null, TEST_ADDRESS_ID);

            assertNotNull(order.getId());
            assertEquals(TEST_USER_ID, order.getUserId());
            assertEquals(0, new BigDecimal("199.98").compareTo(order.getTotalAmount()));
            assertEquals("测试订单备注", order.getRemark());
            assertEquals(0, order.getStatus());

            List<OrderItem> orderItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId())
            );
            assertEquals(1, orderItems.size());
            assertEquals(TEST_PRODUCT_ID_1, orderItems.get(0).getProductId());
            assertEquals(2, orderItems.get(0).getQuantity());

            List<StockReservation> reservations = stockReservationMapper.selectList(
                new LambdaQueryWrapper<StockReservation>().eq(StockReservation::getOrderId, order.getId())
            );
            assertEquals(1, reservations.size());
            assertEquals(2, reservations.get(0).getQuantity());

            assertNull(cartMapper.selectById(cart.getId()));

            Product productAfter = productMapper.selectById(TEST_PRODUCT_ID_1);
            assertEquals(initialStock, productAfter.getStock());
            assertEquals(2, productAfter.getReservedStock());
        }
    }

    @Test
    @DisplayName("购物车结算 - 正常流程（多商品）")
    void testCheckout_MultipleProducts() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            Cart cart1 = cartService.addToCart(TEST_PRODUCT_ID_1, 2);
            Cart cart2 = cartService.addToCart(TEST_PRODUCT_ID_2, 3);

            List<Long> cartIds = new ArrayList<>();
            cartIds.add(cart1.getId());
            cartIds.add(cart2.getId());

            Order order = cartService.checkout(cartIds, null, null, TEST_ADDRESS_ID);

            assertNotNull(order.getId());
            assertEquals(0, new BigDecimal("349.95").compareTo(order.getTotalAmount()));

            List<OrderItem> orderItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId())
            );
            assertEquals(2, orderItems.size());

            List<StockReservation> reservations = stockReservationMapper.selectList(
                new LambdaQueryWrapper<StockReservation>().eq(StockReservation::getOrderId, order.getId())
            );
            assertEquals(2, reservations.size());

            assertNull(cartMapper.selectById(cart1.getId()));
            assertNull(cartMapper.selectById(cart2.getId()));
        }
    }

    @Test
    @DisplayName("购物车结算 - 使用优惠券")
    void testCheckout_WithCoupon() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("admin");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("ADMIN");

            CouponTemplate template = new CouponTemplate();
            template.setName("测试满减券");
            template.setType(1);
            template.setDiscountAmount(new BigDecimal("50"));
            template.setMinAmount(new BigDecimal("200"));
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
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            Cart cart = cartService.addToCart(TEST_PRODUCT_ID_1, 3);

            List<Long> cartIds = new ArrayList<>();
            cartIds.add(cart.getId());

            Order order = cartService.checkout(cartIds, null, userCoupon.getId(), TEST_ADDRESS_ID);

            assertNotNull(order.getId());
            assertEquals(userCoupon.getId(), order.getCouponId());
            assertEquals(0, new BigDecimal("50").compareTo(order.getDiscountAmount()));
            assertEquals(0, new BigDecimal("249.97").compareTo(order.getTotalAmount()));

            UserCoupon usedCoupon = userCouponMapper.selectById(userCoupon.getId());
            assertEquals(1, usedCoupon.getStatus());
            assertEquals(order.getId(), usedCoupon.getOrderId());
        }
    }

    @Test
    @DisplayName("购物车结算 - 未选择商品失败")
    void testCheckout_NoCartItems() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            List<Long> cartIds = new ArrayList<>();

            assertThrows(IllegalArgumentException.class, () ->
                cartService.checkout(cartIds, null, null, TEST_ADDRESS_ID));
        }
    }

    @Test
    @DisplayName("购物车结算 - 购物车项不存在失败")
    void testCheckout_CartItemsNotFound() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            List<Long> cartIds = new ArrayList<>();
            cartIds.add(99999L);

            assertThrows(IllegalArgumentException.class, () ->
                cartService.checkout(cartIds, null, null, TEST_ADDRESS_ID));
        }
    }

    @Test
    @DisplayName("购物车结算 - 商品库存不足失败")
    void testCheckout_InsufficientStock() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            Product product = productMapper.selectById(TEST_PRODUCT_ID_1);
            product.setStock(5);
            product.setReservedStock(4);
            productMapper.updateById(product);

            Cart cart = cartService.addToCart(TEST_PRODUCT_ID_1, 3);

            product.setStock(1);
            product.setReservedStock(0);
            productMapper.updateById(product);

            List<Long> cartIds = new ArrayList<>();
            cartIds.add(cart.getId());

            assertThrows(IllegalArgumentException.class, () ->
                cartService.checkout(cartIds, null, null, TEST_ADDRESS_ID));
        }
    }

    @Test
    @DisplayName("购物车结算 - 事务回滚测试：创建订单失败时购物车不删除")
    void testCheckout_TransactionRollback() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            Cart cart = cartService.addToCart(TEST_PRODUCT_ID_1, 2);

            List<Long> cartIds = new ArrayList<>();
            cartIds.add(cart.getId());

            assertThrows(IllegalArgumentException.class, () ->
                cartService.checkout(cartIds, null, null, 99999L));

            assertNotNull(cartMapper.selectById(cart.getId()));
        }
    }

    @Test
    @DisplayName("购物车结算 - 无权操作他人购物车")
    void testCheckout_NoPermission() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(999L);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("otheruser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            Cart otherCart = new Cart();
            otherCart.setUserId(TEST_USER_ID);
            otherCart.setProductId(TEST_PRODUCT_ID_1);
            otherCart.setQuantity(2);
            cartMapper.insert(otherCart);

            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(999L);

            List<Long> cartIds = new ArrayList<>();
            cartIds.add(otherCart.getId());

            assertThrows(IllegalArgumentException.class, () ->
                cartService.checkout(cartIds, null, null, TEST_ADDRESS_ID));

            assertNotNull(cartMapper.selectById(otherCart.getId()));
        }
    }

    @Test
    @DisplayName("购物车结算 - 收货地址不存在失败")
    void testCheckout_AddressNotFound() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            Cart cart = cartService.addToCart(TEST_PRODUCT_ID_1, 2);

            List<Long> cartIds = new ArrayList<>();
            cartIds.add(cart.getId());

            assertThrows(IllegalArgumentException.class, () ->
                cartService.checkout(cartIds, null, null, 99999L));
        }
    }

    private void ensureTestDataExists() {
        Product product1 = productMapper.selectById(TEST_PRODUCT_ID_1);
        if (product1 == null) {
            product1 = new Product();
            product1.setId(TEST_PRODUCT_ID_1);
            product1.setName("测试商品1");
            product1.setPrice(new BigDecimal("99.99"));
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
            product2.setPrice(new BigDecimal("49.99"));
            product2.setStock(200);
            product2.setReservedStock(0);
            product2.setCategory("测试分类");
            productMapper.insert(product2);
        } else {
            product2.setStock(200);
            product2.setReservedStock(0);
            productMapper.updateById(product2);
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

    private void clearUserCart() {
        cartMapper.delete(new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, TEST_USER_ID));
    }
}
