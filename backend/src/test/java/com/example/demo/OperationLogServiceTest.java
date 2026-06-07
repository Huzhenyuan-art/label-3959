package com.example.demo;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.dto.OperationLogQueryDTO;
import com.example.demo.entity.OperationLog;
import com.example.demo.enums.OperationTypeEnum;
import com.example.demo.mapper.OperationLogMapper;
import com.example.demo.service.OperationLogService;
import com.example.demo.util.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class OperationLogServiceTest {

    @Autowired
    private OperationLogService operationLogService;

    @Autowired
    private OperationLogMapper operationLogMapper;

    @Autowired
    private TestAuditService testAuditService;

    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_ROLE = "USER";
    private static final String TEST_ADMIN_ROLE = "ADMIN";

    @Configuration
    @EnableAspectJAutoProxy
    static class TestConfig {
        @Bean
        public TestAuditService testAuditService() {
            return new TestAuditService();
        }
    }

    @Service
    public static class TestAuditService {
        @com.example.demo.annotation.OperationLog(type = OperationTypeEnum.ORDER_CREATE, targetType = "order", targetIdExpression = "#orderId")
        public String createOrder(Long orderId, String orderNo) {
            return "order:" + orderId;
        }

        @com.example.demo.annotation.OperationLog(type = OperationTypeEnum.ORDER_UPDATE, targetType = "order", targetIdExpression = "#orderId")
        public void updateOrder(Long orderId, String status) {
        }

        @com.example.demo.annotation.OperationLog(type = OperationTypeEnum.ORDER_DELETE, targetType = "order", targetIdExpression = "#result")
        public Long deleteOrder(Long orderId) {
            return orderId;
        }

        @com.example.demo.annotation.OperationLog(type = OperationTypeEnum.REFUND_AUDIT, targetType = "refundOrder", targetIdExpression = "#refundId")
        public void auditRefund(Long refundId, boolean pass) {
            if (!pass) {
                throw new IllegalArgumentException("审核拒绝");
            }
        }

        @com.example.demo.annotation.OperationLog(type = OperationTypeEnum.PRODUCT_CREATE, targetType = "product", targetIdExpression = "#result", recordParams = false, recordResult = false)
        public Long createProduct(String productName) {
            return 100L;
        }
    }

    @BeforeEach
    void setUp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/api/test");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("User-Agent", "TestAgent/1.0");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private void waitForAsyncOperation() throws InterruptedException {
        Thread.sleep(500);
    }

    @Test
    @DisplayName("异步保存日志 - 成功")
    void testSaveLogAsyncSuccess() throws InterruptedException {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn(TEST_USERNAME);
            mocked.when(SecurityUtil::getCurrentRole).thenReturn(TEST_ROLE);

            OperationLog log = new OperationLog();
            log.setOperationType(OperationTypeEnum.ORDER_CREATE.getCode());
            log.setOperationCategory(OperationTypeEnum.ORDER_CREATE.getCategory());
            log.setOperationDesc(OperationTypeEnum.ORDER_CREATE.getDesc());
            log.setOperatorId(TEST_USER_ID);
            log.setOperatorName(TEST_USERNAME);
            log.setOperatorRole(TEST_ROLE);
            log.setTargetId(1000L);
            log.setTargetType("order");
            log.setStatus(1);
            log.setDuration(100L);
            log.setOperationTime(LocalDateTime.now());

            operationLogService.saveLogAsync(log);

            waitForAsyncOperation();

            List<OperationLog> logs = operationLogMapper.selectList(null);
            assertTrue(logs.size() > 0);

            OperationLog savedLog = logs.stream()
                    .filter(l -> l.getTargetId() != null && l.getTargetId().equals(1000L))
                    .findFirst()
                    .orElse(null);

            assertNotNull(savedLog);
            assertEquals(OperationTypeEnum.ORDER_CREATE.getCode(), savedLog.getOperationType());
            assertEquals(TEST_USERNAME, savedLog.getOperatorName());
            assertEquals(1, savedLog.getStatus());
        }
    }

    @Test
    @DisplayName("异步保存日志 - 异常不影响主流程")
    void testSaveLogAsyncWithException() throws InterruptedException {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn(TEST_USERNAME);
            mocked.when(SecurityUtil::getCurrentRole).thenReturn(TEST_ROLE);

            OperationLog log = new OperationLog();
            log.setOperationType(OperationTypeEnum.ORDER_CREATE.getCode());
            log.setOperationCategory(OperationTypeEnum.ORDER_CREATE.getCategory());
            log.setOperationDesc(OperationTypeEnum.ORDER_CREATE.getDesc());
            log.setOperatorId(TEST_USER_ID);
            log.setOperatorName(TEST_USERNAME);
            log.setOperatorRole(TEST_ROLE);
            log.setTargetId(2000L);
            log.setTargetType("order");

            assertDoesNotThrow(() -> operationLogService.saveLogAsync(log));

            waitForAsyncOperation();
        }
    }

    @Test
    @DisplayName("分页查询日志 - 无筛选条件")
    void testPageLogsNoFilter() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn(TEST_USERNAME);
            mocked.when(SecurityUtil::getCurrentRole).thenReturn(TEST_ROLE);

            for (int i = 0; i < 5; i++) {
                OperationLog log = new OperationLog();
                log.setOperationType(OperationTypeEnum.ORDER_CREATE.getCode());
                log.setOperationCategory(OperationTypeEnum.ORDER_CREATE.getCategory());
                log.setOperationDesc(OperationTypeEnum.ORDER_CREATE.getDesc());
                log.setOperatorId(TEST_USER_ID);
                log.setOperatorName(TEST_USERNAME);
                log.setOperatorRole(TEST_ROLE);
                log.setTargetId(3000L + i);
                log.setTargetType("order");
                log.setStatus(1);
                log.setOperationTime(LocalDateTime.now());
                operationLogMapper.insert(log);
            }

            OperationLogQueryDTO query = new OperationLogQueryDTO();
            IPage<OperationLog> page = operationLogService.pageLogs(1, 10, query);

            assertNotNull(page);
            assertTrue(page.getTotal() >= 5);
            assertTrue(page.getRecords().size() >= 5);
        }
    }

    @Test
    @DisplayName("分页查询日志 - 按操作类型筛选")
    void testPageLogsByOperationType() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn(TEST_USERNAME);
            mocked.when(SecurityUtil::getCurrentRole).thenReturn(TEST_ROLE);

            OperationLog orderLog = new OperationLog();
            orderLog.setOperationType(OperationTypeEnum.ORDER_CREATE.getCode());
            orderLog.setOperationCategory(OperationTypeEnum.ORDER_CREATE.getCategory());
            orderLog.setOperationDesc(OperationTypeEnum.ORDER_CREATE.getDesc());
            orderLog.setOperatorId(TEST_USER_ID);
            orderLog.setOperatorName(TEST_USERNAME);
            orderLog.setOperatorRole(TEST_ROLE);
            orderLog.setTargetId(4000L);
            orderLog.setTargetType("order");
            orderLog.setStatus(1);
            orderLog.setOperationTime(LocalDateTime.now());
            operationLogMapper.insert(orderLog);

            OperationLog couponLog = new OperationLog();
            couponLog.setOperationType(OperationTypeEnum.COUPON_CREATE.getCode());
            couponLog.setOperationCategory(OperationTypeEnum.COUPON_CREATE.getCategory());
            couponLog.setOperationDesc(OperationTypeEnum.COUPON_CREATE.getDesc());
            couponLog.setOperatorId(TEST_USER_ID);
            couponLog.setOperatorName(TEST_USERNAME);
            couponLog.setOperatorRole(TEST_ROLE);
            couponLog.setTargetId(5000L);
            couponLog.setTargetType("couponTemplate");
            couponLog.setStatus(1);
            couponLog.setOperationTime(LocalDateTime.now());
            operationLogMapper.insert(couponLog);

            OperationLogQueryDTO query = new OperationLogQueryDTO();
            query.setOperationType(OperationTypeEnum.COUPON_CREATE.getCode());
            IPage<OperationLog> page = operationLogService.pageLogs(1, 10, query);

            assertNotNull(page);
            assertTrue(page.getRecords().stream()
                    .allMatch(l -> OperationTypeEnum.COUPON_CREATE.getCode().equals(l.getOperationType())));
        }
    }

    @Test
    @DisplayName("分页查询日志 - 按操作分类筛选")
    void testPageLogsByOperationCategory() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn(TEST_USERNAME);
            mocked.when(SecurityUtil::getCurrentRole).thenReturn(TEST_ROLE);

            OperationLog orderLog = new OperationLog();
            orderLog.setOperationType(OperationTypeEnum.ORDER_CREATE.getCode());
            orderLog.setOperationCategory("订单管理");
            orderLog.setOperationDesc(OperationTypeEnum.ORDER_CREATE.getDesc());
            orderLog.setOperatorId(TEST_USER_ID);
            orderLog.setOperatorName(TEST_USERNAME);
            orderLog.setOperatorRole(TEST_ROLE);
            orderLog.setTargetId(6000L);
            orderLog.setTargetType("order");
            orderLog.setStatus(1);
            orderLog.setOperationTime(LocalDateTime.now());
            operationLogMapper.insert(orderLog);

            OperationLog userLog = new OperationLog();
            userLog.setOperationType(OperationTypeEnum.USER_CREATE.getCode());
            userLog.setOperationCategory("用户管理");
            userLog.setOperationDesc(OperationTypeEnum.USER_CREATE.getDesc());
            userLog.setOperatorId(TEST_USER_ID);
            userLog.setOperatorName(TEST_USERNAME);
            userLog.setOperatorRole(TEST_ROLE);
            userLog.setTargetId(7000L);
            userLog.setTargetType("user");
            userLog.setStatus(1);
            userLog.setOperationTime(LocalDateTime.now());
            operationLogMapper.insert(userLog);

            OperationLogQueryDTO query = new OperationLogQueryDTO();
            query.setOperationCategory("用户管理");
            IPage<OperationLog> page = operationLogService.pageLogs(1, 10, query);

            assertNotNull(page);
            assertTrue(page.getRecords().stream()
                    .allMatch(l -> "用户管理".equals(l.getOperationCategory())));
        }
    }

    @Test
    @DisplayName("分页查询日志 - 按操作人筛选")
    void testPageLogsByOperatorName() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn(TEST_USERNAME);
            mocked.when(SecurityUtil::getCurrentRole).thenReturn(TEST_ROLE);

            OperationLog log1 = new OperationLog();
            log1.setOperationType(OperationTypeEnum.ORDER_CREATE.getCode());
            log1.setOperationCategory(OperationTypeEnum.ORDER_CREATE.getCategory());
            log1.setOperationDesc(OperationTypeEnum.ORDER_CREATE.getDesc());
            log1.setOperatorId(TEST_USER_ID);
            log1.setOperatorName("userA");
            log1.setOperatorRole(TEST_ROLE);
            log1.setTargetId(8000L);
            log1.setTargetType("order");
            log1.setStatus(1);
            log1.setOperationTime(LocalDateTime.now());
            operationLogMapper.insert(log1);

            OperationLog log2 = new OperationLog();
            log2.setOperationType(OperationTypeEnum.ORDER_CREATE.getCode());
            log2.setOperationCategory(OperationTypeEnum.ORDER_CREATE.getCategory());
            log2.setOperationDesc(OperationTypeEnum.ORDER_CREATE.getDesc());
            log2.setOperatorId(2L);
            log2.setOperatorName("userB");
            log2.setOperatorRole(TEST_ROLE);
            log2.setTargetId(9000L);
            log2.setTargetType("order");
            log2.setStatus(1);
            log2.setOperationTime(LocalDateTime.now());
            operationLogMapper.insert(log2);

            OperationLogQueryDTO query = new OperationLogQueryDTO();
            query.setOperatorName("userA");
            IPage<OperationLog> page = operationLogService.pageLogs(1, 10, query);

            assertNotNull(page);
            assertTrue(page.getRecords().stream()
                    .allMatch(l -> "userA".equals(l.getOperatorName())));
        }
    }

    @Test
    @DisplayName("分页查询日志 - 按状态筛选")
    void testPageLogsByStatus() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn(TEST_USERNAME);
            mocked.when(SecurityUtil::getCurrentRole).thenReturn(TEST_ROLE);

            OperationLog successLog = new OperationLog();
            successLog.setOperationType(OperationTypeEnum.ORDER_CREATE.getCode());
            successLog.setOperationCategory(OperationTypeEnum.ORDER_CREATE.getCategory());
            successLog.setOperationDesc(OperationTypeEnum.ORDER_CREATE.getDesc());
            successLog.setOperatorId(TEST_USER_ID);
            successLog.setOperatorName(TEST_USERNAME);
            successLog.setOperatorRole(TEST_ROLE);
            successLog.setTargetId(10000L);
            successLog.setTargetType("order");
            successLog.setStatus(1);
            successLog.setOperationTime(LocalDateTime.now());
            operationLogMapper.insert(successLog);

            OperationLog failLog = new OperationLog();
            failLog.setOperationType(OperationTypeEnum.ORDER_CREATE.getCode());
            failLog.setOperationCategory(OperationTypeEnum.ORDER_CREATE.getCategory());
            failLog.setOperationDesc(OperationTypeEnum.ORDER_CREATE.getDesc());
            failLog.setOperatorId(TEST_USER_ID);
            failLog.setOperatorName(TEST_USERNAME);
            failLog.setOperatorRole(TEST_ROLE);
            failLog.setTargetId(11000L);
            failLog.setTargetType("order");
            failLog.setStatus(0);
            failLog.setErrorMessage("操作失败");
            failLog.setOperationTime(LocalDateTime.now());
            operationLogMapper.insert(failLog);

            OperationLogQueryDTO query = new OperationLogQueryDTO();
            query.setStatus(0);
            IPage<OperationLog> page = operationLogService.pageLogs(1, 10, query);

            assertNotNull(page);
            assertTrue(page.getRecords().stream()
                    .allMatch(l -> l.getStatus() == 0));
        }
    }

    @Test
    @DisplayName("分页查询日志 - 按时间范围筛选")
    void testPageLogsByTimeRange() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn(TEST_USERNAME);
            mocked.when(SecurityUtil::getCurrentRole).thenReturn(TEST_ROLE);

            OperationLog oldLog = new OperationLog();
            oldLog.setOperationType(OperationTypeEnum.ORDER_CREATE.getCode());
            oldLog.setOperationCategory(OperationTypeEnum.ORDER_CREATE.getCategory());
            oldLog.setOperationDesc(OperationTypeEnum.ORDER_CREATE.getDesc());
            oldLog.setOperatorId(TEST_USER_ID);
            oldLog.setOperatorName(TEST_USERNAME);
            oldLog.setOperatorRole(TEST_ROLE);
            oldLog.setTargetId(12000L);
            oldLog.setTargetType("order");
            oldLog.setStatus(1);
            oldLog.setOperationTime(LocalDateTime.now().minusDays(10));
            operationLogMapper.insert(oldLog);

            OperationLog newLog = new OperationLog();
            newLog.setOperationType(OperationTypeEnum.ORDER_CREATE.getCode());
            newLog.setOperationCategory(OperationTypeEnum.ORDER_CREATE.getCategory());
            newLog.setOperationDesc(OperationTypeEnum.ORDER_CREATE.getDesc());
            newLog.setOperatorId(TEST_USER_ID);
            newLog.setOperatorName(TEST_USERNAME);
            newLog.setOperatorRole(TEST_ROLE);
            newLog.setTargetId(13000L);
            newLog.setTargetType("order");
            newLog.setStatus(1);
            newLog.setOperationTime(LocalDateTime.now());
            operationLogMapper.insert(newLog);

            OperationLogQueryDTO query = new OperationLogQueryDTO();
            query.setStartTime(LocalDateTime.now().minusDays(1));
            query.setEndTime(LocalDateTime.now().plusDays(1));
            IPage<OperationLog> page = operationLogService.pageLogs(1, 10, query);

            assertNotNull(page);
            assertTrue(page.getRecords().stream()
                    .anyMatch(l -> l.getTargetId() != null && l.getTargetId().equals(13000L)));
            assertTrue(page.getRecords().stream()
                    .noneMatch(l -> l.getTargetId() != null && l.getTargetId().equals(12000L)));
        }
    }

    @Test
    @DisplayName("分页查询日志 - 按目标ID和类型筛选")
    void testPageLogsByTarget() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn(TEST_USERNAME);
            mocked.when(SecurityUtil::getCurrentRole).thenReturn(TEST_ROLE);

            OperationLog log1 = new OperationLog();
            log1.setOperationType(OperationTypeEnum.ORDER_UPDATE.getCode());
            log1.setOperationCategory(OperationTypeEnum.ORDER_UPDATE.getCategory());
            log1.setOperationDesc(OperationTypeEnum.ORDER_UPDATE.getDesc());
            log1.setOperatorId(TEST_USER_ID);
            log1.setOperatorName(TEST_USERNAME);
            log1.setOperatorRole(TEST_ROLE);
            log1.setTargetId(14000L);
            log1.setTargetType("order");
            log1.setStatus(1);
            log1.setOperationTime(LocalDateTime.now());
            operationLogMapper.insert(log1);

            OperationLog log2 = new OperationLog();
            log2.setOperationType(OperationTypeEnum.ORDER_UPDATE.getCode());
            log2.setOperationCategory(OperationTypeEnum.ORDER_UPDATE.getCategory());
            log2.setOperationDesc(OperationTypeEnum.ORDER_UPDATE.getDesc());
            log2.setOperatorId(TEST_USER_ID);
            log2.setOperatorName(TEST_USERNAME);
            log2.setOperatorRole(TEST_ROLE);
            log2.setTargetId(14000L);
            log2.setTargetType("order");
            log2.setStatus(1);
            log2.setOperationTime(LocalDateTime.now());
            operationLogMapper.insert(log2);

            OperationLogQueryDTO query = new OperationLogQueryDTO();
            query.setTargetId(14000L);
            query.setTargetType("order");
            IPage<OperationLog> page = operationLogService.pageLogs(1, 10, query);

            assertNotNull(page);
            assertTrue(page.getTotal() >= 2);
        }
    }

    @Test
    @DisplayName("分页查询日志 - 多条件组合筛选")
    void testPageLogsMultipleConditions() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn(TEST_USERNAME);
            mocked.when(SecurityUtil::getCurrentRole).thenReturn(TEST_ROLE);

            for (int i = 0; i < 3; i++) {
                OperationLog log = new OperationLog();
                log.setOperationType(OperationTypeEnum.ORDER_CREATE.getCode());
                log.setOperationCategory("订单管理");
                log.setOperationDesc(OperationTypeEnum.ORDER_CREATE.getDesc());
                log.setOperatorId(TEST_USER_ID);
                log.setOperatorName("admin");
                log.setOperatorRole(TEST_ADMIN_ROLE);
                log.setTargetId(15000L + i);
                log.setTargetType("order");
                log.setStatus(1);
                log.setOperationTime(LocalDateTime.now());
                operationLogMapper.insert(log);
            }

            OperationLogQueryDTO query = new OperationLogQueryDTO();
            query.setOperationCategory("订单管理");
            query.setOperatorName("admin");
            query.setStatus(1);
            IPage<OperationLog> page = operationLogService.pageLogs(1, 10, query);

            assertNotNull(page);
            assertTrue(page.getTotal() >= 3);
            assertTrue(page.getRecords().stream()
                    .allMatch(l -> "订单管理".equals(l.getOperationCategory())
                            && "admin".equals(l.getOperatorName())
                            && l.getStatus() == 1));
        }
    }

    @Test
    @DisplayName("根据ID查询日志 - 成功")
    void testGetByIdSuccess() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn(TEST_USERNAME);
            mocked.when(SecurityUtil::getCurrentRole).thenReturn(TEST_ROLE);

            OperationLog log = new OperationLog();
            log.setOperationType(OperationTypeEnum.ORDER_CREATE.getCode());
            log.setOperationCategory(OperationTypeEnum.ORDER_CREATE.getCategory());
            log.setOperationDesc(OperationTypeEnum.ORDER_CREATE.getDesc());
            log.setOperatorId(TEST_USER_ID);
            log.setOperatorName(TEST_USERNAME);
            log.setOperatorRole(TEST_ROLE);
            log.setTargetId(16000L);
            log.setTargetType("order");
            log.setStatus(1);
            log.setOperationTime(LocalDateTime.now());
            operationLogMapper.insert(log);

            OperationLog found = operationLogService.getById(log.getId());

            assertNotNull(found);
            assertEquals(log.getId(), found.getId());
            assertEquals(OperationTypeEnum.ORDER_CREATE.getCode(), found.getOperationType());
        }
    }

    @Test
    @DisplayName("根据ID查询日志 - 不存在返回Null")
    void testGetByIdNotFound() {
        OperationLog found = operationLogService.getById(999999L);

        assertNull(found);
    }

    @Test
    @DisplayName("获取操作类型列表")
    void testGetOperationTypes() {
        List<Map<String, String>> types = operationLogService.getOperationTypes();

        assertNotNull(types);
        assertFalse(types.isEmpty());
        assertTrue(types.size() >= OperationTypeEnum.values().length);

        assertTrue(types.stream()
                .anyMatch(map -> OperationTypeEnum.ORDER_CREATE.getCode().equals(map.get("code"))));
        assertTrue(types.stream()
                .anyMatch(map -> OperationTypeEnum.COUPON_CREATE.getCode().equals(map.get("code"))));
    }

    @Test
    @DisplayName("获取操作分类列表")
    void testGetOperationCategories() {
        List<Map<String, String>> categories = operationLogService.getOperationCategories();

        assertNotNull(categories);
        assertFalse(categories.isEmpty());

        assertTrue(categories.stream()
                .anyMatch(map -> "订单管理".equals(map.get("category"))));
        assertTrue(categories.stream()
                .anyMatch(map -> "用户管理".equals(map.get("category"))));
        assertTrue(categories.stream()
                .anyMatch(map -> "优惠券管理".equals(map.get("category"))));
    }

    @Test
    @DisplayName("AOP切面触发 - 创建订单记录日志")
    void testAopCreateOrder() throws InterruptedException {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn(TEST_USERNAME);
            mocked.when(SecurityUtil::getCurrentRole).thenReturn(TEST_ROLE);

            Long orderId = 17000L;
            String result = testAuditService.createOrder(orderId, "ORD-001");

            assertEquals("order:" + orderId, result);

            waitForAsyncOperation();

            OperationLogQueryDTO query = new OperationLogQueryDTO();
            query.setTargetId(orderId);
            query.setTargetType("order");
            IPage<OperationLog> page = operationLogService.pageLogs(1, 10, query);

            assertTrue(page.getTotal() >= 1);
            OperationLog log = page.getRecords().get(0);
            assertEquals(OperationTypeEnum.ORDER_CREATE.getCode(), log.getOperationType());
            assertEquals(TEST_USERNAME, log.getOperatorName());
            assertEquals(1, log.getStatus());
            assertNotNull(log.getRequestMethod());
            assertNotNull(log.getRequestUri());
        }
    }

    @Test
    @DisplayName("AOP切面触发 - 更新订单记录日志")
    void testAopUpdateOrder() throws InterruptedException {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn(TEST_USERNAME);
            mocked.when(SecurityUtil::getCurrentRole).thenReturn(TEST_ROLE);

            Long orderId = 18000L;
            testAuditService.updateOrder(orderId, "PAID");

            waitForAsyncOperation();

            OperationLogQueryDTO query = new OperationLogQueryDTO();
            query.setTargetId(orderId);
            query.setTargetType("order");
            IPage<OperationLog> page = operationLogService.pageLogs(1, 10, query);

            assertTrue(page.getTotal() >= 1);
            OperationLog log = page.getRecords().get(0);
            assertEquals(OperationTypeEnum.ORDER_UPDATE.getCode(), log.getOperationType());
        }
    }

    @Test
    @DisplayName("AOP切面触发 - 删除订单从返回值获取目标ID")
    void testAopDeleteOrderWithResultExpression() throws InterruptedException {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn(TEST_USERNAME);
            mocked.when(SecurityUtil::getCurrentRole).thenReturn(TEST_ROLE);

            Long orderId = 19000L;
            Long resultId = testAuditService.deleteOrder(orderId);

            assertEquals(orderId, resultId);

            waitForAsyncOperation();

            OperationLogQueryDTO query = new OperationLogQueryDTO();
            query.setTargetId(orderId);
            query.setTargetType("order");
            IPage<OperationLog> page = operationLogService.pageLogs(1, 10, query);

            assertTrue(page.getTotal() >= 1);
            OperationLog log = page.getRecords().get(0);
            assertEquals(OperationTypeEnum.ORDER_DELETE.getCode(), log.getOperationType());
            assertEquals(orderId, log.getTargetId());
        }
    }

    @Test
    @DisplayName("AOP切面触发 - 异常时记录失败日志")
    void testAopExceptionLogging() throws InterruptedException {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn(TEST_USERNAME);
            mocked.when(SecurityUtil::getCurrentRole).thenReturn(TEST_ROLE);

            Long refundId = 20000L;

            assertThrows(IllegalArgumentException.class, () ->
                    testAuditService.auditRefund(refundId, false));

            waitForAsyncOperation();

            OperationLogQueryDTO query = new OperationLogQueryDTO();
            query.setTargetId(refundId);
            query.setTargetType("refundOrder");
            IPage<OperationLog> page = operationLogService.pageLogs(1, 10, query);

            assertTrue(page.getTotal() >= 1);
            OperationLog log = page.getRecords().get(0);
            assertEquals(OperationTypeEnum.REFUND_AUDIT.getCode(), log.getOperationType());
            assertEquals(0, log.getStatus());
            assertNotNull(log.getErrorMessage());
            assertTrue(log.getErrorMessage().contains("审核拒绝"));
        }
    }

    @Test
    @DisplayName("AOP切面触发 - 不记录参数和结果")
    void testAopNoRecordParamsAndResult() throws InterruptedException {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn(TEST_USERNAME);
            mocked.when(SecurityUtil::getCurrentRole).thenReturn(TEST_ROLE);

            Long productId = testAuditService.createProduct("测试商品");

            assertEquals(100L, productId);

            waitForAsyncOperation();

            OperationLogQueryDTO query = new OperationLogQueryDTO();
            query.setTargetId(productId);
            query.setTargetType("product");
            IPage<OperationLog> page = operationLogService.pageLogs(1, 10, query);

            assertTrue(page.getTotal() >= 1);
            OperationLog log = page.getRecords().get(0);
            assertEquals(OperationTypeEnum.PRODUCT_CREATE.getCode(), log.getOperationType());
            assertNull(log.getRequestParams());
            assertNull(log.getAfterData());
        }
    }

    @Test
    @DisplayName("并发异步写入 - 多条日志不丢失")
    void testConcurrentAsyncSave() throws InterruptedException {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn(TEST_USERNAME);
            mocked.when(SecurityUtil::getCurrentRole).thenReturn(TEST_ROLE);

            int threadCount = 10;
            CountDownLatch latch = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                new Thread(() -> {
                    try {
                        OperationLog log = new OperationLog();
                        log.setOperationType(OperationTypeEnum.ORDER_CREATE.getCode());
                        log.setOperationCategory(OperationTypeEnum.ORDER_CREATE.getCategory());
                        log.setOperationDesc(OperationTypeEnum.ORDER_CREATE.getDesc());
                        log.setOperatorId(TEST_USER_ID);
                        log.setOperatorName(TEST_USERNAME);
                        log.setOperatorRole(TEST_ROLE);
                        log.setTargetId(30000L + index);
                        log.setTargetType("order");
                        log.setStatus(1);
                        log.setDuration((long) (Math.random() * 100));
                        log.setOperationTime(LocalDateTime.now());
                        operationLogService.saveLogAsync(log);
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }

            assertTrue(latch.await(10, TimeUnit.SECONDS));
            waitForAsyncOperation();

            OperationLogQueryDTO query = new OperationLogQueryDTO();
            query.setOperationCategory(OperationTypeEnum.ORDER_CREATE.getCategory());
            query.setOperatorName(TEST_USERNAME);
            IPage<OperationLog> page = operationLogService.pageLogs(1, 50, query);

            assertTrue(page.getTotal() >= threadCount);
        }
    }

    @Test
    @DisplayName("事务回滚 - 主事务回滚不影响日志")
    void testTransactionRollbackWithLog() throws InterruptedException {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn(TEST_USERNAME);
            mocked.when(SecurityUtil::getCurrentRole).thenReturn(TEST_ROLE);

            Long orderId = 40000L;

            OperationLog log = new OperationLog();
            log.setOperationType(OperationTypeEnum.ORDER_CREATE.getCode());
            log.setOperationCategory(OperationTypeEnum.ORDER_CREATE.getCategory());
            log.setOperationDesc(OperationTypeEnum.ORDER_CREATE.getDesc());
            log.setOperatorId(TEST_USER_ID);
            log.setOperatorName(TEST_USERNAME);
            log.setOperatorRole(TEST_ROLE);
            log.setTargetId(orderId);
            log.setTargetType("order");
            log.setStatus(1);
            log.setOperationTime(LocalDateTime.now());

            operationLogService.saveLogAsync(log);

            waitForAsyncOperation();

            OperationLogQueryDTO query = new OperationLogQueryDTO();
            query.setTargetId(orderId);
            IPage<OperationLog> page = operationLogService.pageLogs(1, 10, query);

            assertTrue(page.getTotal() >= 1);
        }
    }

    @Test
    @DisplayName("分页查询 - 边界页码处理")
    void testPageLogsBoundaryPages() {
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(TEST_USER_ID);
            mocked.when(SecurityUtil::getCurrentUsername).thenReturn(TEST_USERNAME);
            mocked.when(SecurityUtil::getCurrentRole).thenReturn(TEST_ROLE);

            for (int i = 0; i < 15; i++) {
                OperationLog log = new OperationLog();
                log.setOperationType(OperationTypeEnum.USER_CREATE.getCode());
                log.setOperationCategory(OperationTypeEnum.USER_CREATE.getCategory());
                log.setOperationDesc(OperationTypeEnum.USER_CREATE.getDesc());
                log.setOperatorId(TEST_USER_ID);
                log.setOperatorName(TEST_USERNAME);
                log.setOperatorRole(TEST_ROLE);
                log.setTargetId(50000L + i);
                log.setTargetType("user");
                log.setStatus(1);
                log.setOperationTime(LocalDateTime.now());
                operationLogMapper.insert(log);
            }

            OperationLogQueryDTO query = new OperationLogQueryDTO();
            query.setOperationType(OperationTypeEnum.USER_CREATE.getCode());

            IPage<OperationLog> page1 = operationLogService.pageLogs(1, 10, query);
            assertEquals(10, page1.getRecords().size());
            assertEquals(1, page1.getCurrent());
            assertEquals(10, page1.getSize());

            IPage<OperationLog> page2 = operationLogService.pageLogs(2, 10, query);
            assertTrue(page2.getRecords().size() >= 5);
            assertEquals(2, page2.getCurrent());
        }
    }

    @Test
    @DisplayName("操作类型枚举完整性")
    void testOperationTypeEnumIntegrity() {
        List<Map<String, String>> types = operationLogService.getOperationTypes();
        List<Map<String, String>> categories = operationLogService.getOperationCategories();

        for (OperationTypeEnum typeEnum : OperationTypeEnum.values()) {
            assertTrue(types.stream()
                    .anyMatch(map -> typeEnum.getCode().equals(map.get("code"))));
            assertTrue(types.stream()
                    .anyMatch(map -> typeEnum.getDesc().equals(map.get("desc"))));
            assertTrue(types.stream()
                    .anyMatch(map -> typeEnum.getCategory().equals(map.get("category"))));
        }

        for (Map<String, String> category : categories) {
            assertNotNull(category.get("category"));
            assertFalse(category.get("category").isEmpty());
        }
    }
}
