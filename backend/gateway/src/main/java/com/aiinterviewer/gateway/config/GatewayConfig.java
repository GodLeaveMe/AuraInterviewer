package com.aiinterviewer.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

/**
 * Gateway配置类
 */
@Configuration
public class GatewayConfig {

    /**
     * 基于IP的限流Key解析器
     */
    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(
            exchange.getRequest()
                .getRemoteAddress()
                .getAddress()
                .getHostAddress()
        );
    }

    /**
     * 基于用户ID的限流Key解析器（备用）
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> exchange.getRequest()
            .getHeaders()
            .getFirst("user-id") != null
                ? Mono.just(exchange.getRequest().getHeaders().getFirst("user-id"))
                : Mono.just("anonymous");
    }
}
