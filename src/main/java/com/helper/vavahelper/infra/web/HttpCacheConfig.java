package com.helper.vavahelper.infra.web;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

/**
 * Adiciona ETag nas rotas publicas de leitura. Se o cliente enviar If-None-Match com o mesmo valor,
 * a API responde 304 sem corpo, poupando banda do servidor e do cliente.
 */
@Configuration
public class HttpCacheConfig {

    @Bean
    public FilterRegistrationBean<ShallowEtagHeaderFilter> etagFilter() {
        FilterRegistrationBean<ShallowEtagHeaderFilter> registration =
                new FilterRegistrationBean<>(new ShallowEtagHeaderFilter());
        registration.addUrlPatterns("/agents", "/agents/*", "/media", "/media/*");
        registration.setName("etagFilter");
        return registration;
    }
}
