package com.example.demo;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.dto.CouponCreateDTO;
import com.example.demo.dto.CouponUseResultDTO;
import com.example.demo.entity.CouponTemplate;
import com.example.demo.entity.UserCoupon;
import com.example.demo.enums.CouponTemplateStatusEnum;
import com.example.demo.enums.CouponTypeEnum;
import com.example.demo.enums.UserCouponStatusEnum;
import com.example.demo.mapper.CouponTemplateMapper;
import com.example.demo.mapper.UserCouponMapper;
import com.example.demo.service.CouponService;
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
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class CouponServiceTest {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponTemplateMapper couponTemplateMapper;

    @Autowired
    private UserCouponMapper userCouponMapper;

    private static final Long TEST_USER_ID = 1L;

    @BeforeEach
    void setUp() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);
        }
    }

    @Test
    @DisplayName("创建满减券模板 - 成功")
    void testCreateFixedAmountCouponTemplate() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);

            CouponCreateDTO dto = new CouponCreateDTO();
            dto.setName("测试满减券");
            dto.setType(CouponTypeEnum.FIXED_AMOUNT.getCode());
            dto.setDiscountAmount(new BigDecimal("10"));
            dto.setMinAmount(new BigDecimal("100"));
            dto.setTotalCount(100);
            dto.setPerUserLimit(2);
            dto.setValidStartTime(LocalDateTime.now().minusDays(1));
            dto.setValidEndTime(LocalDateTime.now().plusDays(30));
            dto.setDescription("满100减10");

            CouponTemplate template = couponService.createTemplate(dto);

            assertNotNull(template.getId());
            assertEquals("测试满减券", template.getName());
            assertEquals(CouponTypeEnum.FIXED_AMOUNT.getCode(), template.getType());
            assertEquals(0, new BigDecimal("10").compareTo(template.getDiscountAmount()));
            assertEquals(0, new BigDecimal("100").compareTo(template.getMinAmount()));
            assertEquals(100, template.getTotalCount());
            assertEquals(2, template.getPerUserLimit());
            assertEquals(CouponTemplateStatusEnum.ACTIVE.getCode(), template.getStatus());
        }
    }

    @Test
    @DisplayName("创建折扣券模板 - 成功")
    void testCreateDiscountRateCouponTemplate() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);

            CouponCreateDTO dto = new CouponCreateDTO();
            dto.setName("测试折扣券");
            dto.setType(CouponTypeEnum.DISCOUNT_RATE.getCode());
            dto.setDiscountRate(new BigDecimal("0.8"));
            dto.setMinAmount(new BigDecimal("200"));
            dto.setTotalCount(50);
            dto.setPerUserLimit(1);
            dto.setValidDays(7);
            dto.setDescription("8折优惠");

            CouponTemplate template = couponService.createTemplate(dto);

            assertNotNull(template.getId());
            assertEquals("测试折扣券", template.getName());
            assertEquals(CouponTypeEnum.DISCOUNT_RATE.getCode(), template.getType());
            assertEquals(0, new BigDecimal("0.8").compareTo(template.getDiscountRate()));
            assertEquals(7, template.getValidDays());
        }
    }

    @Test
    @DisplayName("创建券模板 - 减免金额为负数失败")
    void testCreateTemplateWithNegativeDiscount() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);

            CouponCreateDTO dto = new CouponCreateDTO();
            dto.setName("测试券");
            dto.setType(CouponTypeEnum.FIXED_AMOUNT.getCode());
            dto.setDiscountAmount(new BigDecimal("-10"));
            dto.setMinAmount(BigDecimal.ZERO);
            dto.setTotalCount(100);
            dto.setPerUserLimit(1);
            dto.setValidDays(7);

            assertThrows(IllegalArgumentException.class, () -> couponService.createTemplate(dto));
        }
    }

    @Test
    @DisplayName("创建券模板 - 折扣率超出范围失败")
    void testCreateTemplateWithInvalidDiscountRate() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);

            CouponCreateDTO dto = new CouponCreateDTO();
            dto.setName("测试券");
            dto.setType(CouponTypeEnum.DISCOUNT_RATE.getCode());
            dto.setDiscountRate(new BigDecimal("1.5"));
            dto.setMinAmount(BigDecimal.ZERO);
            dto.setTotalCount(100);
            dto.setPerUserLimit(1);
            dto.setValidDays(7);

            assertThrows(IllegalArgumentException.class, () -> couponService.createTemplate(dto));
        }
    }

    @Test
    @DisplayName("创建券模板 - 未设置有效期失败")
    void testCreateTemplateWithoutValidity() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);

            CouponCreateDTO dto = new CouponCreateDTO();
            dto.setName("测试券");
            dto.setType(CouponTypeEnum.FIXED_AMOUNT.getCode());
            dto.setDiscountAmount(new BigDecimal("10"));
            dto.setMinAmount(BigDecimal.ZERO);
            dto.setTotalCount(100);
            dto.setPerUserLimit(1);

            assertThrows(IllegalArgumentException.class, () -> couponService.createTemplate(dto));
        }
    }

    @Test
    @DisplayName("用户领取优惠券 - 成功")
    void testReceiveCoupon() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);

            CouponTemplate template = createTestTemplate();

            mocked.when(SecurityUtil::isAdmin).thenReturn(false);

            UserCoupon userCoupon = couponService.receiveCoupon(template.getId());

            assertNotNull(userCoupon.getId());
            assertEquals(TEST_USER_ID, userCoupon.getUserId());
            assertEquals(template.getId(), userCoupon.getTemplateId());
            assertEquals(UserCouponStatusEnum.AVAILABLE.getCode(), userCoupon.getStatus());
            assertNotNull(userCoupon.getCouponCode());
            assertTrue(userCoupon.getCouponCode().startsWith("CP"));

            CouponTemplate updatedTemplate = couponTemplateMapper.selectById(template.getId());
            assertEquals(1, updatedTemplate.getReceivedCount());
        }
    }

    @Test
    @DisplayName("用户领取优惠券 - 超过限领次数失败")
    void testReceiveCouponExceedLimit() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);

            CouponTemplate template = createTestTemplate();

            mocked.when(SecurityUtil::isAdmin).thenReturn(false);

            couponService.receiveCoupon(template.getId());
            couponService.receiveCoupon(template.getId());

            assertThrows(IllegalArgumentException.class, () -> couponService.receiveCoupon(template.getId()));
        }
    }

    @Test
    @DisplayName("用户领取优惠券 - 已停用模板失败")
    void testReceiveCouponDisabledTemplate() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);

            CouponTemplate template = createTestTemplate();
            couponService.updateTemplateStatus(template.getId(), CouponTemplateStatusEnum.DISABLED.getCode());

            mocked.when(SecurityUtil::isAdmin).thenReturn(false);

            assertThrows(IllegalArgumentException.class, () -> couponService.receiveCoupon(template.getId()));
        }
    }

    @Test
    @DisplayName("计算满减券折扣 - 成功")
    void testCalculateFixedAmountDiscount() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);

            CouponTemplate template = createTestTemplate();

            mocked.when(SecurityUtil::isAdmin).thenReturn(false);

            UserCoupon userCoupon = couponService.receiveCoupon(template.getId());

            CouponUseResultDTO result = couponService.calculateDiscount(userCoupon.getId(), new BigDecimal("150"));

            assertEquals(0, new BigDecimal("10").compareTo(result.getDiscountAmount()));
            assertEquals(0, new BigDecimal("150").compareTo(result.getOriginalAmount()));
            assertEquals(0, new BigDecimal("140").compareTo(result.getFinalAmount()));
        }
    }

    @Test
    @DisplayName("计算折扣券折扣 - 成功")
    void testCalculateDiscountRateDiscount() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);

            CouponCreateDTO dto = new CouponCreateDTO();
            dto.setName("8折券");
            dto.setType(CouponTypeEnum.DISCOUNT_RATE.getCode());
            dto.setDiscountRate(new BigDecimal("0.8"));
            dto.setMinAmount(new BigDecimal("100"));
            dto.setTotalCount(100);
            dto.setPerUserLimit(2);
            dto.setValidDays(7);
            CouponTemplate template = couponService.createTemplate(dto);

            mocked.when(SecurityUtil::isAdmin).thenReturn(false);

            UserCoupon userCoupon = couponService.receiveCoupon(template.getId());

            CouponUseResultDTO result = couponService.calculateDiscount(userCoupon.getId(), new BigDecimal("200"));

            assertEquals(0, new BigDecimal("40.00").compareTo(result.getDiscountAmount()));
            assertEquals(0, new BigDecimal("160.00").compareTo(result.getFinalAmount()));
        }
    }

    @Test
    @DisplayName("计算折扣 - 订单金额未达门槛失败")
    void testCalculateDiscountBelowMinAmount() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);

            CouponTemplate template = createTestTemplate();

            mocked.when(SecurityUtil::isAdmin).thenReturn(false);

            UserCoupon userCoupon = couponService.receiveCoupon(template.getId());

            assertThrows(IllegalArgumentException.class, () ->
                    couponService.calculateDiscount(userCoupon.getId(), new BigDecimal("50")));
        }
    }

    @Test
    @DisplayName("使用优惠券 - 成功")
    void testUseCoupon() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);

            CouponTemplate template = createTestTemplate();

            mocked.when(SecurityUtil::isAdmin).thenReturn(false);

            UserCoupon userCoupon = couponService.receiveCoupon(template.getId());
            Long orderId = 100L;

            UserCoupon usedCoupon = couponService.useCoupon(userCoupon.getId(), orderId, new BigDecimal("150"));

            assertEquals(UserCouponStatusEnum.USED.getCode(), usedCoupon.getStatus());
            assertNotNull(usedCoupon.getUsedTime());
            assertEquals(orderId, usedCoupon.getOrderId());
            assertEquals(0, new BigDecimal("10").compareTo(usedCoupon.getDiscountAmount()));

            CouponTemplate updatedTemplate = couponTemplateMapper.selectById(template.getId());
            assertEquals(1, updatedTemplate.getUsedCount());
        }
    }

    @Test
    @DisplayName("使用优惠券 - 已使用的券失败")
    void testUseAlreadyUsedCoupon() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);

            CouponTemplate template = createTestTemplate();

            mocked.when(SecurityUtil::isAdmin).thenReturn(false);

            UserCoupon userCoupon = couponService.receiveCoupon(template.getId());
            couponService.useCoupon(userCoupon.getId(), 100L, new BigDecimal("150"));

            assertThrows(IllegalArgumentException.class, () ->
                    couponService.useCoupon(userCoupon.getId(), 101L, new BigDecimal("200")));
        }
    }

    @Test
    @DisplayName("恢复优惠券 - 订单取消成功")
    void testRestoreCoupon() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);

            CouponTemplate template = createTestTemplate();

            mocked.when(SecurityUtil::isAdmin).thenReturn(false);

            UserCoupon userCoupon = couponService.receiveCoupon(template.getId());
            Long orderId = 100L;
            couponService.useCoupon(userCoupon.getId(), orderId, new BigDecimal("150"));

            couponService.restoreCoupon(orderId);

            UserCoupon restoredCoupon = userCouponMapper.selectById(userCoupon.getId());
            assertEquals(UserCouponStatusEnum.AVAILABLE.getCode(), restoredCoupon.getStatus());
            assertNull(restoredCoupon.getUsedTime());
            assertNull(restoredCoupon.getOrderId());
            assertNull(restoredCoupon.getDiscountAmount());

            CouponTemplate updatedTemplate = couponTemplateMapper.selectById(template.getId());
            assertEquals(0, updatedTemplate.getUsedCount());
        }
    }

    @Test
    @DisplayName("使用优惠券 - 折扣金额不超过订单金额")
    void testUseCouponDiscountNotExceedOrderAmount() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);

            CouponCreateDTO dto = new CouponCreateDTO();
            dto.setName("大面额券");
            dto.setType(CouponTypeEnum.FIXED_AMOUNT.getCode());
            dto.setDiscountAmount(new BigDecimal("200"));
            dto.setMinAmount(new BigDecimal("100"));
            dto.setTotalCount(100);
            dto.setPerUserLimit(2);
            dto.setValidDays(7);
            CouponTemplate template = couponService.createTemplate(dto);

            mocked.when(SecurityUtil::isAdmin).thenReturn(false);

            UserCoupon userCoupon = couponService.receiveCoupon(template.getId());

            CouponUseResultDTO result = couponService.calculateDiscount(userCoupon.getId(), new BigDecimal("100"));

            assertEquals(0, new BigDecimal("100").compareTo(result.getDiscountAmount()));
            assertEquals(0, BigDecimal.ZERO.compareTo(result.getFinalAmount()));
        }
    }

    @Test
    @DisplayName("自动过期处理 - 过期优惠券状态更新")
    void testExpireCoupons() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);

            CouponTemplate template = createTestTemplate();

            mocked.when(SecurityUtil::isAdmin).thenReturn(false);

            UserCoupon userCoupon = couponService.receiveCoupon(template.getId());

            userCoupon.setValidEndTime(LocalDateTime.now().minusDays(1));
            userCouponMapper.updateById(userCoupon);

            userCouponMapper.expireCoupons(LocalDateTime.now());

            UserCoupon expiredCoupon = userCouponMapper.selectById(userCoupon.getId());
            assertEquals(UserCouponStatusEnum.EXPIRED.getCode(), expiredCoupon.getStatus());
        }
    }

    private CouponTemplate createTestTemplate() {
        CouponCreateDTO dto = new CouponCreateDTO();
        dto.setName("测试满减券");
        dto.setType(CouponTypeEnum.FIXED_AMOUNT.getCode());
        dto.setDiscountAmount(new BigDecimal("10"));
        dto.setMinAmount(new BigDecimal("100"));
        dto.setTotalCount(100);
        dto.setPerUserLimit(2);
        dto.setValidStartTime(LocalDateTime.now().minusDays(1));
        dto.setValidEndTime(LocalDateTime.now().plusDays(30));
        return couponService.createTemplate(dto);
    }
}
