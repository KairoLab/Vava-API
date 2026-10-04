package com.helper.vavahelper.infra.cache;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Liga o cache do Spring. O CacheManager (Caffeine) e configurado por propriedades:
 * spring.cache.type, spring.cache.cache-names e spring.cache.caffeine.spec.
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
