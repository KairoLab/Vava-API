package com.helper.vavahelper.infra.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuracao do rate limit (prefixo app.rate-limit).
 * Cada regra define quantas requisicoes cabem na janela, por IP.
 */
@Data
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;

    /** Todas as rotas que nao tem regra propria. */
    private Rule global = new Rule(120, 60);

    /** POST /auth/login */
    private Rule login = new Rule(10, 60);

    /** POST /auth/register */
    private Rule register = new Rule(10, 3600);

    /** POST /auth/forgot-password e /auth/reset-password */
    private Rule recovery = new Rule(5, 900);

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Rule {
        private int capacity;
        private long windowSeconds;
    }
}
