package com.logistics.ai.stream.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.WebFilter;

/**
 * Cấu hình WebFlux nâng cao: Tự động bổ sung HTTP Header `X-Accel-Buffering: no`
 * cho toàn bộ response để ngăn cản Reverse Proxy (Nginx, HAProxy) đệm dữ liệu stream.
 */
@Configuration
public class WebFluxStreamConfig {

    @Bean
    public WebFilter disableNginxBufferingFilter() {
        return (exchange, chain) -> {
            exchange.getResponse().getHeaders().add("X-Accel-Buffering", "no");
            exchange.getResponse().getHeaders().add(HttpHeaders.CACHE_CONTROL, "no-cache");
            return chain.filter(exchange);
        };
    }
}
