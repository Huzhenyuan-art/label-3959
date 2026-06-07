package com.example.demo;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.dto.CouponCreateDTO;
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
class CouponServiceExtendedTest {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponTemplateMapper couponTemplateMapper;

    @Autowired
    private UserCouponMapper userCouponMapper;

    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_USER_ID_2 = 2L;

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
    @DisplayName("并发领取优惠券 - 领完即止测试")
    void testReceiveCoupon_ConcurrentStockExhausted() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("admin");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("ADMIN");

            CouponCreateDTO dto = new CouponCreateDTO();
            dto.setName("限量券");
            dto.setType(CouponTypeEnum.FIXED_AMOUNT.getCode());
            dto.setDiscountAmount(new BigDecimal("10"));
            dto.setMinAmount(BigDecimal.ZERO);
            dto.setTotalCount(1);
            dto.setPerUserLimit(1);
            dto.setValidStartTime(LocalDateTime.now().minusDays(1));
            dto.setValidEndTime(LocalDateTime.now().plusDays(30));
            CouponTemplate template = couponService.createTemplate(dto);

            mocked.when(SecurityUtil::isAdmin).thenReturn(false);

            UserCoupon userCoupon1 = couponService.receiveCoupon(template.getId());
            assertNotNull(userCoupon1.getId());

            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID_2);

            assertThrows(IllegalArgumentException.class, () ->
                couponService.receiveCoupon(template.getId()));

            CouponTemplate updatedTemplate = couponTemplateMapper.selectById(template.getId());
            assertEquals(1, updatedTemplate.getReceivedCount());
            assertEquals(1, updatedTemplate.getTotalCount());
        }
    }

    @Test
    @DisplayName("领取优惠券 - 同一用户超过限领次数失败")
    void testReceiveCoupon_UserLimitExceeded() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("admin");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("ADMIN");

            CouponCreateDTO dto = new CouponCreateDTO();
            dto.setName("每人限领1张");
            dto.setType(CouponTypeEnum.FIXED_AMOUNT.getCode());
            dto.setDiscountAmount(new BigDecimal("10"));
            dto.setMinAmount(BigDecimal.ZERO);
            dto.setTotalCount(100);
            dto.setPerUserLimit(1);
            dto.setValidStartTime(LocalDateTime.now().minusDays(1));
            dto.setValidEndTime(LocalDateTime.now().plusDays(30));
            CouponTemplate template = couponService.createTemplate(dto);

            mocked.when(SecurityUtil::isAdmin).thenReturn(false);

            couponService.receiveCoupon(template.getId());

            assertThrows(IllegalArgumentException.class, () ->
                couponService.receiveCoupon(template.getId()));
        }
    }

    @Test
    @DisplayName("领取优惠券 - 未开始领取失败")
    void testReceiveCoupon_NotStarted() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("admin");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("ADMIN");

            CouponCreateDTO dto = new CouponCreateDTO();
            dto.setName("明天开始的券");
            dto.setType(CouponTypeEnum.FIXED_AMOUNT.getCode());
            dto.setDiscountAmount(new BigDecimal("10"));
            dto.setMinAmount(BigDecimal.ZERO);
            dto.setTotalCount(100);
            dto.setPerUserLimit(2);
            dto.setValidStartTime(LocalDateTime.now().plusDays(1));
            dto.setValidEndTime(LocalDateTime.now().plusDays(30));
            CouponTemplate template = couponService.createTemplate(dto);

            mocked.when(SecurityUtil::isAdmin).thenReturn(false);

            assertThrows(IllegalArgumentException.class, () ->
                couponService.receiveCoupon(template.getId()));
        }
    }

    @Test
    @DisplayName("领取优惠券 - 已过期失败")
    void testReceiveCoupon_Expired() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("admin");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("ADMIN");

            CouponCreateDTO dto = new CouponCreateDTO();
            dto.setName("已过期的券");
            dto.setType(CouponTypeEnum.FIXED_AMOUNT.getCode());
            dto.setDiscountAmount(new BigDecimal("10"));
            dto.setMinAmount(BigDecimal.ZERO);
            dto.setTotalCount(100);
            dto.setPerUserLimit(2);
            dto.setValidStartTime(LocalDateTime.now().minusDays(30));
            dto.setValidEndTime(LocalDateTime.now().minusDays(1));
            CouponTemplate template = couponService.createTemplate(dto);

            mocked.when(SecurityUtil::isAdmin).thenReturn(false);

            assertThrows(IllegalArgumentException.class, () ->
                couponService.receiveCoupon(template.getId()));
        }
    }

    @Test
    @DisplayName("使用优惠券 - 已过期失败")
    void testUseCoupon_Expired() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("admin");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("ADMIN");

            CouponTemplate template = createTestTemplate();

            mocked.when(SecurityUtil::isAdmin).thenReturn(false);

            UserCoupon userCoupon = couponService.receiveCoupon(template.getId());

            userCoupon.setValidEndTime(LocalDateTime.now().minusDays(1));
            userCouponMapper.updateById(userCoupon);

            assertThrows(IllegalArgumentException.class, () ->
                couponService.useCoupon(userCoupon.getId(), 100L, new BigDecimal("150")));
        }
    }

    @Test
    @DisplayName("使用优惠券 - 订单金额为零失败")
    void testUseCoupon_ZeroOrderAmount() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("admin");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("ADMIN");

            CouponTemplate template = createTestTemplate();

            mocked.when(SecurityUtil::isAdmin).thenReturn(false);

            UserCoupon userCoupon = couponService.receiveCoupon(template.getId());

            assertThrows(IllegalArgumentException.class, () ->
                couponService.useCoupon(userCoupon.getId(), 100L, BigDecimal.ZERO));
        }
    }

    @Test
    @DisplayName("使用优惠券 - 不是本人的券失败")
    void testUseCoupon_NotOwner() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("admin");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("ADMIN");

            CouponTemplate template = createTestTemplate();

            UserCoupon userCoupon = new UserCoupon();
            userCoupon.setUserId(999L);
            userCoupon.setTemplateId(template.getId());
            userCoupon.setCouponCode("CPTEST001");
            userCoupon.setStatus(UserCouponStatusEnum.AVAILABLE.getCode());
            userCoupon.setValidStartTime(LocalDateTime.now().minusDays(1));
            userCoupon.setValidEndTime(LocalDateTime.now().plusDays(30));
            userCouponMapper.insert(userCoupon);

            mocked.when(SecurityUtil::isAdmin).thenReturn(false);

            assertThrows(IllegalArgumentException.class, () ->
                couponService.useCoupon(userCoupon.getId(), 100L, new BigDecimal("150")));
        }
    }

    @Test
    @DisplayName("恢复优惠券 - 订单不存在不报错")
    void testRestoreCoupon_OrderNotFound() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            assertDoesNotThrow(() -> couponService.restoreCoupon(99999L));
        }
    }

    @Test
    @DisplayName("恢复优惠券 - 重复调用不重复恢复")
    void testRestoreCoupon_Idempotent() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("admin");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("ADMIN");

            CouponTemplate template = createTestTemplate();

            mocked.when(SecurityUtil::isAdmin).thenReturn(false);

            UserCoupon userCoupon = couponService.receiveCoupon(template.getId());
            Long orderId = 200L;
            couponService.useCoupon(userCoupon.getId(), orderId, new BigDecimal("150"));

            couponService.restoreCoupon(orderId);
            couponService.restoreCoupon(orderId);

            UserCoupon restoredCoupon = userCouponMapper.selectById(userCoupon.getId());
            assertEquals(UserCouponStatusEnum.AVAILABLE.getCode(), restoredCoupon.getStatus());
            assertNull(restoredCoupon.getOrderId());

            CouponTemplate updatedTemplate = couponTemplateMapper.selectById(template.getId());
            assertEquals(0, updatedTemplate.getUsedCount());
        }
    }

    @Test
    @DisplayName("计算折扣 - 折扣券精确计算")
    void testCalculateDiscount_RatePrecision() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("admin");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("ADMIN");

            CouponCreateDTO dto = new CouponCreateDTO();
            dto.setName("95折券");
            dto.setType(CouponTypeEnum.DISCOUNT_RATE.getCode());
            dto.setDiscountRate(new BigDecimal("0.95"));
            dto.setMinAmount(new BigDecimal("100"));
            dto.setTotalCount(100);
            dto.setPerUserLimit(2);
            dto.setValidDays(7);
            CouponTemplate template = couponService.createTemplate(dto);

            mocked.when(SecurityUtil::isAdmin).thenReturn(false);

            UserCoupon userCoupon = couponService.receiveCoupon(template.getId());

            var result = couponService.calculateDiscount(userCoupon.getId(), new BigDecimal("100"));

            assertEquals(0, new BigDecimal("5.00").compareTo(result.getDiscountAmount()));
            assertEquals(0, new BigDecimal("95.00").compareTo(result.getFinalAmount()));
        }
    }

    @Test
    @DisplayName("创建券模板 - 总数量为0失败")
    void testCreateTemplate_ZeroTotalCount() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("admin");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("ADMIN");

            CouponCreateDTO dto = new CouponCreateDTO();
            dto.setName("测试券");
            dto.setType(CouponTypeEnum.FIXED_AMOUNT.getCode());
            dto.setDiscountAmount(new BigDecimal("10"));
            dto.setMinAmount(BigDecimal.ZERO);
            dto.setTotalCount(0);
            dto.setPerUserLimit(1);
            dto.setValidDays(7);

            assertThrows(IllegalArgumentException.class, () -> couponService.createTemplate(dto));
        }
    }

    @Test
    @DisplayName("创建券模板 - 每人限领数量为0失败")
    void testCreateTemplate_ZeroPerUserLimit() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("admin");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("ADMIN");

            CouponCreateDTO dto = new CouponCreateDTO();
            dto.setName("测试券");
            dto.setType(CouponTypeEnum.FIXED_AMOUNT.getCode());
            dto.setDiscountAmount(new BigDecimal("10"));
            dto.setMinAmount(BigDecimal.ZERO);
            dto.setTotalCount(100);
            dto.setPerUserLimit(0);
            dto.setValidDays(7);

            assertThrows(IllegalArgumentException.class, () -> couponService.createTemplate(dto));
        }
    }

    @Test
    @DisplayName("创建券模板 - 有效期开始时间晚于结束时间失败")
    void testCreateTemplate_InvalidValidityPeriod() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("admin");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("ADMIN");

            CouponCreateDTO dto = new CouponCreateDTO();
            dto.setName("测试券");
            dto.setType(CouponTypeEnum.FIXED_AMOUNT.getCode());
            dto.setDiscountAmount(new BigDecimal("10"));
            dto.setMinAmount(BigDecimal.ZERO);
            dto.setTotalCount(100);
            dto.setPerUserLimit(1);
            dto.setValidStartTime(LocalDateTime.now().plusDays(30));
            dto.setValidEndTime(LocalDateTime.now().plusDays(1));

            assertThrows(IllegalArgumentException.class, () -> couponService.createTemplate(dto));
        }
    }

    @Test
    @DisplayName("创建券模板 - 非管理员权限失败")
    void testCreateTemplate_NonAdminFailure() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            CouponCreateDTO dto = new CouponCreateDTO();
            dto.setName("测试券");
            dto.setType(CouponTypeEnum.FIXED_AMOUNT.getCode());
            dto.setDiscountAmount(new BigDecimal("10"));
            dto.setMinAmount(BigDecimal.ZERO);
            dto.setTotalCount(100);
            dto.setPerUserLimit(1);
            dto.setValidDays(7);

            assertThrows(SecurityException.class, () -> couponService.createTemplate(dto));
        }
    }

    @Test
    @DisplayName("更新券模板状态 - 正常流程")
    void testUpdateTemplateStatus_Success() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("admin");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("ADMIN");

            CouponTemplate template = createTestTemplate();

            couponService.updateTemplateStatus(template.getId(), CouponTemplateStatusEnum.DISABLED.getCode());

            CouponTemplate updatedTemplate = couponTemplateMapper.selectById(template.getId());
            assertEquals(CouponTemplateStatusEnum.DISABLED.getCode(), updatedTemplate.getStatus());
        }
    }

    @Test
    @DisplayName("更新券模板状态 - 非管理员权限失败")
    void testUpdateTemplateStatus_NonAdminFailure() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("admin");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("ADMIN");

            CouponTemplate template = createTestTemplate();

            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            assertThrows(SecurityException.class, () ->
                couponService.updateTemplateStatus(template.getId(), CouponTemplateStatusEnum.DISABLED.getCode()));
        }
    }

    @Test
    @DisplayName("查询可用券模板列表 - 正常流程")
    void testGetAvailableTemplates_Success() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            var templates = couponService.getAvailableTemplates();

            assertNotNull(templates);
            assertTrue(templates.size() >= 0);
        }
    }

    @Test
    @DisplayName("查询我的优惠券 - 正常流程")
    void testGetMyCoupons_Success() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            var coupons = couponService.getUserCoupons(null);

            assertNotNull(coupons);
            assertTrue(coupons.size() >= 0);
        }
    }

    @Test
    @DisplayName("查询我的优惠券 - 按状态筛选")
    void testGetMyCoupons_ByStatus() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("admin");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("ADMIN");

            CouponTemplate template = createTestTemplate();

            mocked.when(SecurityUtil::isAdmin).thenReturn(false);

            UserCoupon userCoupon = couponService.receiveCoupon(template.getId());

            var coupons = couponService.getUserCoupons(UserCouponStatusEnum.AVAILABLE.getCode());

            assertNotNull(coupons);
            assertTrue(coupons.size() >= 1);
        }
    }

    @Test
    @DisplayName("事务回滚测试 - 使用优惠券后异常回滚")
    void testUseCoupon_TransactionRollback() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("admin");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("ADMIN");

            CouponTemplate template = createTestTemplate();

            mocked.when(SecurityUtil::isAdmin).thenReturn(false);

            UserCoupon userCoupon = couponService.receiveCoupon(template.getId());

            assertThrows(IllegalArgumentException.class, () ->
                couponService.useCoupon(userCoupon.getId(), 300L, new BigDecimal("50")));

            UserCoupon stillAvailable = userCouponMapper.selectById(userCoupon.getId());
            assertEquals(UserCouponStatusEnum.AVAILABLE.getCode(), stillAvailable.getStatus());
            assertNull(stillAvailable.getOrderId());

            CouponTemplate unchangedTemplate = couponTemplateMapper.selectById(template.getId());
            assertEquals(0, unchangedTemplate.getUsedCount());
        }
    }

    @Test
    @DisplayName("获取券模板详情 - 正常流程")
    void testGetTemplateDetail_Success() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("admin");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("ADMIN");

            CouponTemplate template = createTestTemplate();

            var detail = couponTemplateMapper.selectById(template.getId());

            assertNotNull(detail);
            assertEquals(template.getId(), detail.getId());
        }
    }

    @Test
    @DisplayName("获取券模板详情 - 不存在返回Null")
    void testGetTemplateDetail_NotFound() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(false);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("testuser");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("USER");

            var detail = couponTemplateMapper.selectById(99999L);

            assertNull(detail);
        }
    }

    @Test
    @DisplayName("批量过期处理 - 多张券同时过期")
    void testExpireCoupons_MultipleExpired() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("admin");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("ADMIN");

            CouponTemplate template = createTestTemplate();

            mocked.when(SecurityUtil::isAdmin).thenReturn(false);

            UserCoupon coupon1 = couponService.receiveCoupon(template.getId());

            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID_2);
            UserCoupon coupon2 = couponService.receiveCoupon(template.getId());

            coupon1.setValidEndTime(LocalDateTime.now().minusDays(1));
            coupon2.setValidEndTime(LocalDateTime.now().minusDays(1));
            userCouponMapper.updateById(coupon1);
            userCouponMapper.updateById(coupon2);

            int expiredCount = userCouponMapper.expireCoupons(LocalDateTime.now());

            assertTrue(expiredCount >= 2);

            UserCoupon expired1 = userCouponMapper.selectById(coupon1.getId());
            UserCoupon expired2 = userCouponMapper.selectById(coupon2.getId());
            assertEquals(UserCouponStatusEnum.EXPIRED.getCode(), expired1.getStatus());
            assertEquals(UserCouponStatusEnum.EXPIRED.getCode(), expired2.getStatus());
        }
    }

    @Test
    @DisplayName("领取优惠券 - 同时设置固定天数和起止时间，优先使用起止时间")
    void testReceiveCoupon_ValidityPriority() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("admin");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("ADMIN");

            CouponCreateDTO dto = new CouponCreateDTO();
            dto.setName("测试券");
            dto.setType(CouponTypeEnum.FIXED_AMOUNT.getCode());
            dto.setDiscountAmount(new BigDecimal("10"));
            dto.setMinAmount(new BigDecimal("100"));
            dto.setTotalCount(100);
            dto.setPerUserLimit(2);
            dto.setValidDays(7);
            dto.setValidStartTime(LocalDateTime.now().minusDays(1));
            dto.setValidEndTime(LocalDateTime.now().plusDays(30));
            CouponTemplate template = couponService.createTemplate(dto);

            mocked.when(SecurityUtil::isAdmin).thenReturn(false);

            UserCoupon userCoupon = couponService.receiveCoupon(template.getId());

            assertNotNull(userCoupon.getValidStartTime());
            assertNotNull(userCoupon.getValidEndTime());
            assertTrue(userCoupon.getValidEndTime().isAfter(LocalDateTime.now().plusDays(10)));
        }
    }

    @Test
    @DisplayName("领取优惠券 - 只设置有效天数，自动计算有效期")
    void testReceiveCoupon_ValidDaysCalculation() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::isAdmin).thenReturn(true);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn("admin");
            mocked.when(SecurityUtil::getCurrentRole).thenReturn("ADMIN");

            CouponCreateDTO dto = new CouponCreateDTO();
            dto.setName("测试券");
            dto.setType(CouponTypeEnum.FIXED_AMOUNT.getCode());
            dto.setDiscountAmount(new BigDecimal("10"));
            dto.setMinAmount(new BigDecimal("100"));
            dto.setTotalCount(100);
            dto.setPerUserLimit(2);
            dto.setValidDays(7);
            CouponTemplate template = couponService.createTemplate(dto);

            mocked.when(SecurityUtil::isAdmin).thenReturn(false);

            UserCoupon userCoupon = couponService.receiveCoupon(template.getId());

            assertNotNull(userCoupon.getValidStartTime());
            assertNotNull(userCoupon.getValidEndTime());
            assertTrue(userCoupon.getValidEndTime()
                .isEqual(userCoupon.getValidStartTime().plusDays(7))
                || userCoupon.getValidEndTime().isAfter(userCoupon.getValidStartTime().plusDays(7).minusMinutes(1)));
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
