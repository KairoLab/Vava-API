package com.helper.vavahelper.infra.ratelimit;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Limita requisicoes por IP antes de qualquer outro processamento (inclusive o Spring Security).
 * O IP vem de request.getRemoteAddr(); atras de proxy confiavel o Tomcat ja o ajusta a partir de
 * X-Forwarded-For (server.forward-headers-strategy=native), entao o cabecalho nao e lido aqui
 * diretamente e nao pode ser forjado por clientes externos.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@ConditionalOnProperty(prefix = "app.rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long MAX_TRACKED_CLIENTS = 100_000;

    private enum Bucket { GLOBAL, LOGIN, REGISTER, RECOVERY }

    private final RateLimitProperties props;
    private final Cache<String, TokenBucket> buckets;

    public RateLimitFilter(RateLimitProperties props) {
        this.props = props;
        long longestWindow = Math.max(
                Math.max(props.getGlobal().getWindowSeconds(), props.getLogin().getWindowSeconds()),
                Math.max(props.getRegister().getWindowSeconds(), props.getRecovery().getWindowSeconds()));
        // Um bucket ocioso por uma janela inteira ja esta cheio, entao pode ser descartado.
        this.buckets = Caffeine.newBuilder()
                .maximumSize(MAX_TRACKED_CLIENTS)
                .expireAfterAccess(Duration.ofSeconds(longestWindow * 2))
                .build();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Preflight de CORS e health check de plataforma nao entram na contagem.
        return HttpMethod.OPTIONS.matches(request.getMethod()) || path.startsWith("/actuator/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        Bucket bucket = classify(request);
        RateLimitProperties.Rule rule = ruleFor(bucket);

        String key = bucket.name() + ":" + request.getRemoteAddr();
        long now = System.nanoTime();
        long windowNanos = TimeUnit.SECONDS.toNanos(Math.max(1, rule.getWindowSeconds()));
        TokenBucket tb = buckets.get(key, k -> new TokenBucket(Math.max(1, rule.getCapacity()), windowNanos, now));

        long waitNanos = tb.tryConsume(now);
        response.setHeader("X-RateLimit-Limit", String.valueOf(rule.getCapacity()));

        if (waitNanos == 0L) {
            response.setHeader("X-RateLimit-Remaining", String.valueOf(tb.remaining()));
            chain.doFilter(request, response);
            return;
        }

        long retryAfter = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(waitNanos) + 1);
        log.warn("Rate limit atingido: regra={} ip={} metodo={} caminho={}",
                bucket, request.getRemoteAddr(), request.getMethod(), request.getRequestURI());

        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(retryAfter));
        response.setHeader("X-RateLimit-Remaining", "0");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\":\"too_many_requests\",\"message\":\"Muitas requisicoes. Tente novamente em "
                + retryAfter + " segundos.\"}");
    }

    private Bucket classify(HttpServletRequest request) {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return Bucket.GLOBAL;
        }
        String path = request.getRequestURI();
        if (path.equals("/auth/login")) return Bucket.LOGIN;
        if (path.equals("/auth/register")) return Bucket.REGISTER;
        if (path.equals("/auth/forgot-password") || path.equals("/auth/reset-password")) return Bucket.RECOVERY;
        return Bucket.GLOBAL;
    }

    private RateLimitProperties.Rule ruleFor(Bucket bucket) {
        return switch (bucket) {
            case LOGIN -> props.getLogin();
            case REGISTER -> props.getRegister();
            case RECOVERY -> props.getRecovery();
            case GLOBAL -> props.getGlobal();
        };
    }
}
