package com.helper.vavahelper.infra.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitFilterTest {

    private MockHttpServletResponse call(RateLimitFilter filter, String method, String path, String ip) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    @Test
    void blocksLoginAfterLimitAndSendsRetryAfter() throws Exception {
        RateLimitProperties props = new RateLimitProperties();
        props.setLogin(new RateLimitProperties.Rule(3, 60));
        RateLimitFilter filter = new RateLimitFilter(props);

        for (int i = 0; i < 3; i++) {
            assertEquals(200, call(filter, "POST", "/auth/login", "10.0.0.1").getStatus());
        }
        MockHttpServletResponse blocked = call(filter, "POST", "/auth/login", "10.0.0.1");
        assertEquals(429, blocked.getStatus());
        assertNotNull(blocked.getHeader("Retry-After"));
    }

    @Test
    void limitsAreIndependentPerIp() throws Exception {
        RateLimitProperties props = new RateLimitProperties();
        props.setLogin(new RateLimitProperties.Rule(1, 60));
        RateLimitFilter filter = new RateLimitFilter(props);

        assertEquals(200, call(filter, "POST", "/auth/login", "10.0.0.1").getStatus());
        assertEquals(429, call(filter, "POST", "/auth/login", "10.0.0.1").getStatus());
        assertEquals(200, call(filter, "POST", "/auth/login", "10.0.0.2").getStatus());
    }

    @Test
    void authBucketsDoNotConsumeGlobalBucket() throws Exception {
        RateLimitProperties props = new RateLimitProperties();
        props.setLogin(new RateLimitProperties.Rule(1, 60));
        props.setGlobal(new RateLimitProperties.Rule(2, 60));
        RateLimitFilter filter = new RateLimitFilter(props);

        call(filter, "POST", "/auth/login", "10.0.0.1");
        assertEquals(200, call(filter, "GET", "/agents", "10.0.0.1").getStatus());
        assertEquals(200, call(filter, "GET", "/agents", "10.0.0.1").getStatus());
        assertEquals(429, call(filter, "GET", "/agents", "10.0.0.1").getStatus());
    }

    @Test
    void healthCheckAndPreflightAreNeverLimited() throws Exception {
        RateLimitProperties props = new RateLimitProperties();
        props.setGlobal(new RateLimitProperties.Rule(1, 60));
        RateLimitFilter filter = new RateLimitFilter(props);

        for (int i = 0; i < 5; i++) {
            assertEquals(200, call(filter, "GET", "/actuator/health", "10.0.0.1").getStatus());
            assertEquals(200, call(filter, "OPTIONS", "/agents", "10.0.0.1").getStatus());
        }
    }
}
