package com.cheche.login.security;

import static org.junit.jupiter.api.Assertions.*;

import com.cheche.login.domain.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceTest {
    private final JwtService service = new JwtService(
            "test-secret-key-must-be-at-least-32-bytes-long", 3600, "cheche-login-service");

    @Test
    void tokenContainsIdentityButNotMutableAuthorizationData() {
        User user = new User("regional-admin", "encoded-password");
        ReflectionTestUtils.setField(user, "id", 7L);

        Claims claims = service.parse(service.issue(user));

        assertEquals(7L, ((Number) claims.get("userId")).longValue());
        assertEquals("regional-admin", claims.get("username"));
        assertEquals("ADMIN", claims.get("accountType"));
        assertNull(claims.get("role"));
        assertNull(claims.get("regionCode"));
    }
}
