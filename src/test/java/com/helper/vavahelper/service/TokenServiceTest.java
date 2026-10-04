package com.helper.vavahelper.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.helper.vavahelper.models.User.User;
import com.helper.vavahelper.models.User.UserRole;

class TokenServiceTest {

    private static final String SECRET = "unit-test-secret-with-more-than-32-chars!";

    private TokenService service(String secret, long minutes) {
        TokenService service = new TokenService();
        ReflectionTestUtils.setField(service, "secret", secret);
        ReflectionTestUtils.setField(service, "expirationMinutes", minutes);
        service.init();
        return service;
    }

    @Test
    void generatesAndValidatesToken() {
        TokenService service = service(SECRET, 5);
        String token = service.generateToken(new User("a@b.com", "hash", UserRole.USER));
        assertEquals("a@b.com", service.validateToken(token));
    }

    @Test
    void invalidTokenReturnsNullInsteadOfThrowing() {
        TokenService service = service(SECRET, 5);
        assertNull(service.validateToken("not-a-jwt"));
    }

    @Test
    void expiredTokenIsRejected() {
        TokenService service = service(SECRET, -10);
        String token = service.generateToken(new User("a@b.com", "hash", UserRole.USER));
        assertNull(service.validateToken(token));
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        String token = service(SECRET, 5).generateToken(new User("a@b.com", "hash", UserRole.USER));
        TokenService other = service("another-secret-with-more-than-32-chars!!", 5);
        assertNull(other.validateToken(token));
    }

    @Test
    void weakOrKnownSecretsPreventStartup() {
        assertThrows(IllegalStateException.class, () -> service("my-secret-key", 5));
        assertThrows(IllegalStateException.class, () -> service("short", 5));
        assertThrows(IllegalStateException.class, () -> service(null, 5));
    }
}
