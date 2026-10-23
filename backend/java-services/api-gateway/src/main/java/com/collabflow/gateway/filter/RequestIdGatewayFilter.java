package com.collabflow.gateway.filter;

import java.util.UUID;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Ensure every request has {@code X-Request-Id} (generate if missing) and echo it on the response.
 */
@Component
public class RequestIdGatewayFilter implements GlobalFilter, Ordered {

    public static final String HEADER_NAME = "X-Request-Id";

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestId = exchange.getRequest().getHeaders().getFirst(HEADER_NAME);
        if (!StringUtils.hasText(requestId)) {
            requestId = UUID.randomUUID().toString();
        }
        String finalId = requestId;
        ServerHttpRequest request = exchange.getRequest().mutate()
                .header(HEADER_NAME, finalId)
                .build();
        exchange.getResponse().getHeaders().set(HEADER_NAME, finalId);
        return chain.filter(exchange.mutate().request(request).build());
    }
}
