package com.helper.vavahelper.infra.ratelimit;

/**
 * Token bucket simples e thread-safe. Comeca cheio e reabastece de forma continua,
 * de modo que "capacity" tokens sao repostos a cada "windowNanos".
 */
final class TokenBucket {

    private final long capacity;
    private final double refillPerNano;
    private double tokens;
    private long lastRefill;

    TokenBucket(long capacity, long windowNanos, long nowNanos) {
        this.capacity = capacity;
        this.refillPerNano = (double) capacity / windowNanos;
        this.tokens = capacity;
        this.lastRefill = nowNanos;
    }

    /**
     * Tenta consumir um token.
     *
     * @return 0 se consumiu; caso contrario, quantos nanossegundos faltam para haver um token.
     */
    synchronized long tryConsume(long nowNanos) {
        double elapsed = Math.max(0, nowNanos - lastRefill);
        tokens = Math.min(capacity, tokens + elapsed * refillPerNano);
        lastRefill = nowNanos;
        if (tokens >= 1.0) {
            tokens -= 1.0;
            return 0L;
        }
        return (long) Math.ceil((1.0 - tokens) / refillPerNano);
    }

    synchronized long remaining() {
        return (long) Math.floor(tokens);
    }
}
