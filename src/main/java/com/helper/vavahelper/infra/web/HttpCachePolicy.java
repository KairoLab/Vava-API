package com.helper.vavahelper.infra.web;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.stereotype.Component;

/** Politica de Cache-Control das rotas publicas de leitura (app.http-cache.max-age-seconds). */
@Component
public class HttpCachePolicy {

    private final CacheControl cacheControl;

    public HttpCachePolicy(@Value("${app.http-cache.max-age-seconds:300}") long maxAgeSeconds) {
        this.cacheControl = maxAgeSeconds > 0
                ? CacheControl.maxAge(Duration.ofSeconds(maxAgeSeconds)).cachePublic()
                : CacheControl.noStore();
    }

    public CacheControl get() {
        return cacheControl;
    }
}
