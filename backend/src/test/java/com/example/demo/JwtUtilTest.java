package com.example.demo;

import com.example.demo.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class JwtUtilTest {

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${jwt.secret:mybatis-plus-demo-secret-key-1234567890abcdef}")
    private String testSecret;

    @Value("${jwt.expire-hours:24}")
    private int testExpireHours;

    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_ROLE_USER = "USER";
    private static final String TEST_ROLE_ADMIN = "ADMIN";
    private static final Long TEST_USER_ID = 100L;

    @Test
    @DisplayName("生成Token - 普通用户角色")
    void testGenerateTokenWithUserRole() {
        String token = jwtUtil.generateToken(TEST_USERNAME, TEST_ROLE_USER, TEST_USER_ID);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3);
    }

    @Test
    @DisplayName("生成Token - 管理员角色")
    void testGenerateTokenWithAdminRole() {
        String token = jwtUtil.generateToken("admin", TEST_ROLE_ADMIN, 1L);

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    @DisplayName("生成Token - 不同用户ID")
    void testGenerateTokenWithDifferentUserId() {
        String token1 = jwtUtil.generateToken(TEST_USERNAME, TEST_ROLE_USER, 1L);
        String token2 = jwtUtil.generateToken(TEST_USERNAME, TEST_ROLE_USER, 2L);

        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1, token2);
    }

    @Test
    @DisplayName("解析Token - 提取用户名")
    void testGetUsernameFromToken() {
        String token = jwtUtil.generateToken(TEST_USERNAME, TEST_ROLE_USER, TEST_USER_ID);

        String username = jwtUtil.getUsername(token);

        assertEquals(TEST_USERNAME, username);
    }

    @Test
    @DisplayName("解析Token - 提取用户角色")
    void testGetRoleFromToken() {
        String token = jwtUtil.generateToken(TEST_USERNAME, TEST_ROLE_ADMIN, TEST_USER_ID);

        String role = jwtUtil.getRole(token);

        assertEquals(TEST_ROLE_ADMIN, role);
    }

    @Test
    @DisplayName("解析Token - 提取用户ID")
    void testGetUserIdFromToken() {
        String token = jwtUtil.generateToken(TEST_USERNAME, TEST_ROLE_USER, TEST_USER_ID);

        Long userId = jwtUtil.getUserId(token);

        assertEquals(TEST_USER_ID, userId);
    }

    @Test
    @DisplayName("解析Token - 完整Claims信息")
    void testParseTokenFullClaims() {
        String token = jwtUtil.generateToken(TEST_USERNAME, TEST_ROLE_USER, TEST_USER_ID);

        Claims claims = jwtUtil.parseToken(token);

        assertEquals(TEST_USERNAME, claims.getSubject());
        assertEquals(TEST_ROLE_USER, claims.get("role", String.class));
        assertEquals(TEST_USER_ID, claims.get("userId", Long.class));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
    }

    @Test
    @DisplayName("验证Token - 有效Token")
    void testValidateTokenWithValidToken() {
        String token = jwtUtil.generateToken(TEST_USERNAME, TEST_ROLE_USER, TEST_USER_ID);

        boolean isValid = jwtUtil.validateToken(token);

        assertTrue(isValid);
    }

    @Test
    @DisplayName("验证Token - 空Token失败")
    void testValidateTokenWithEmptyToken() {
        boolean isValid = jwtUtil.validateToken("");

        assertFalse(isValid);
    }

    @Test
    @DisplayName("验证Token - Null Token失败")
    void testValidateTokenWithNullToken() {
        boolean isValid = jwtUtil.validateToken(null);

        assertFalse(isValid);
    }

    @Test
    @DisplayName("验证Token - 伪造Token失败")
    void testValidateTokenWithFakeToken() {
        String fakeToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

        boolean isValid = jwtUtil.validateToken(fakeToken);

        assertFalse(isValid);
    }

    @Test
    @DisplayName("验证Token - 篡改签名失败")
    void testValidateTokenWithTamperedSignature() {
        String originalToken = jwtUtil.generateToken(TEST_USERNAME, TEST_ROLE_USER, TEST_USER_ID);
        String[] parts = originalToken.split("\\.");
        String tamperedToken = parts[0] + "." + parts[1] + ".tamperedSignature";

        boolean isValid = jwtUtil.validateToken(tamperedToken);

        assertFalse(isValid);
    }

    @Test
    @DisplayName("验证Token - 过期Token失败")
    void testValidateTokenWithExpiredToken() throws InterruptedException {
        JwtUtil shortLivedJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(shortLivedJwtUtil, "secret", testSecret);
        ReflectionTestUtils.setField(shortLivedJwtUtil, "expireHours", 0);

        String token = shortLivedJwtUtil.generateToken(TEST_USERNAME, TEST_ROLE_USER, TEST_USER_ID);

        Thread.sleep(100);

        boolean isValid = shortLivedJwtUtil.validateToken(token);

        assertFalse(isValid);
    }

    @Test
    @DisplayName("验证Token - 错误密钥签名失败")
    void testValidateTokenWithWrongSecret() {
        String wrongSecret = "wrong-secret-key-1234567890abcdefghijklmnop";
        byte[] keyBytes = wrongSecret.getBytes(StandardCharsets.UTF_8);
        SecretKey wrongSignKey = Keys.hmacShaKeyFor(keyBytes);

        Date now = new Date();
        Date expireDate = new Date(now.getTime() + testExpireHours * 3600L * 1000);
        String tokenSignedWithWrongKey = Jwts.builder()
                .subject(TEST_USERNAME)
                .claim("role", TEST_ROLE_USER)
                .claim("userId", TEST_USER_ID)
                .issuedAt(now)
                .expiration(expireDate)
                .signWith(wrongSignKey)
                .compact();

        boolean isValid = jwtUtil.validateToken(tokenSignedWithWrongKey);

        assertFalse(isValid);
    }

    @Test
    @DisplayName("Token过期时间 - 验证有效期设置")
    void testTokenExpirationTime() {
        String token = jwtUtil.generateToken(TEST_USERNAME, TEST_ROLE_USER, TEST_USER_ID);

        Claims claims = jwtUtil.parseToken(token);
        Date issuedAt = claims.getIssuedAt();
        Date expiration = claims.getExpiration();

        assertNotNull(issuedAt);
        assertNotNull(expiration);

        long expectedExpireMs = testExpireHours * 3600L * 1000;
        long actualExpireMs = expiration.getTime() - issuedAt.getTime();
        long toleranceMs = 1000L;

        assertTrue(Math.abs(actualExpireMs - expectedExpireMs) < toleranceMs);
    }

    @Test
    @DisplayName("Token并发生成 - 多个Token互不相同")
    void testGenerateMultipleTokensConcurrently() {
        String token1 = jwtUtil.generateToken("user1", TEST_ROLE_USER, 1L);
        String token2 = jwtUtil.generateToken("user2", TEST_ROLE_ADMIN, 2L);
        String token3 = jwtUtil.generateToken("user3", TEST_ROLE_USER, 3L);

        assertNotEquals(token1, token2);
        assertNotEquals(token2, token3);
        assertNotEquals(token1, token3);

        assertTrue(jwtUtil.validateToken(token1));
        assertTrue(jwtUtil.validateToken(token2));
        assertTrue(jwtUtil.validateToken(token3));
    }

    @Test
    @DisplayName("解析Token - 用户ID边界值测试")
    void testGetUserIdBoundaryValues() {
        Long minUserId = 1L;
        Long maxUserId = Long.MAX_VALUE;

        String token1 = jwtUtil.generateToken(TEST_USERNAME, TEST_ROLE_USER, minUserId);
        String token2 = jwtUtil.generateToken(TEST_USERNAME, TEST_ROLE_USER, maxUserId);

        assertEquals(minUserId, jwtUtil.getUserId(token1));
        assertEquals(maxUserId, jwtUtil.getUserId(token2));
    }

    @Test
    @DisplayName("解析Token - 空角色处理")
    void testGetRoleWithEmptyRole() {
        String token = jwtUtil.generateToken(TEST_USERNAME, "", TEST_USER_ID);

        String role = jwtUtil.getRole(token);

        assertEquals("", role);
    }

    @Test
    @DisplayName("验证Token - 格式错误Token失败")
    void testValidateTokenWithMalformedToken() {
        boolean isValid1 = jwtUtil.validateToken("invalid.token");
        boolean isValid2 = jwtUtil.validateToken("invalid");
        boolean isValid3 = jwtUtil.validateToken("a.b.c.d");

        assertFalse(isValid1);
        assertFalse(isValid2);
        assertFalse(isValid3);
    }

    @Test
    @DisplayName("Token生成 - 特殊字符用户名")
    void testGenerateTokenWithSpecialCharacters() {
        String specialUsername = "user@#$%^&*()_+-=[]{}|;':\",./<>?";
        String token = jwtUtil.generateToken(specialUsername, TEST_ROLE_USER, TEST_USER_ID);

        assertNotNull(token);
        assertTrue(jwtUtil.validateToken(token));
        assertEquals(specialUsername, jwtUtil.getUsername(token));
    }

    @Test
    @DisplayName("Token解析 - 长用户名处理")
    void testGenerateTokenWithLongUsername() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("a");
        }
        String longUsername = sb.toString();

        String token = jwtUtil.generateToken(longUsername, TEST_ROLE_USER, TEST_USER_ID);

        assertNotNull(token);
        assertTrue(jwtUtil.validateToken(token));
        assertEquals(longUsername, jwtUtil.getUsername(token));
    }

    @Test
    @DisplayName("Token完整性 - 内容不被篡改")
    void testTokenIntegrity() {
        String originalToken = jwtUtil.generateToken(TEST_USERNAME, TEST_ROLE_USER, TEST_USER_ID);
        String originalUsername = jwtUtil.getUsername(originalToken);
        String originalRole = jwtUtil.getRole(originalToken);
        Long originalUserId = jwtUtil.getUserId(originalToken);

        for (int i = 0; i < 5; i++) {
            assertEquals(originalUsername, jwtUtil.getUsername(originalToken));
            assertEquals(originalRole, jwtUtil.getRole(originalToken));
            assertEquals(originalUserId, jwtUtil.getUserId(originalToken));
        }
    }

    @Test
    @DisplayName("权限校验 - 管理员角色Token")
    void testAdminRoleTokenValidation() {
        String adminToken = jwtUtil.generateToken("admin", TEST_ROLE_ADMIN, 1L);

        assertTrue(jwtUtil.validateToken(adminToken));
        assertEquals(TEST_ROLE_ADMIN, jwtUtil.getRole(adminToken));
    }

    @Test
    @DisplayName("权限校验 - 普通用户角色Token")
    void testUserRoleTokenValidation() {
        String userToken = jwtUtil.generateToken(TEST_USERNAME, TEST_ROLE_USER, TEST_USER_ID);

        assertTrue(jwtUtil.validateToken(userToken));
        assertEquals(TEST_ROLE_USER, jwtUtil.getRole(userToken));
        assertNotEquals(TEST_ROLE_ADMIN, jwtUtil.getRole(userToken));
    }
}
