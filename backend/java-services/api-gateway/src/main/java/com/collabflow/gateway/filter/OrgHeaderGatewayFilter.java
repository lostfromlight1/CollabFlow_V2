package com.collabflow.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Explicitly forward {@code X-Organization-Id} to upstreams when the client sent it.
 * Services must still validate membership — this only preserves the header across the proxy.
 */
@Component
public class OrgHeaderGatewayFilter implements GlobalFilter, Ordered {

    public static final String HEADER_NAME = "X-Organization-Id";

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String orgId = exchange.getRequest().getHeaders().getFirst(HEADER_NAME);
        if (!StringUtils.hasText(orgId)) {
            return chain.filter(exchange);
        }
        ServerHttpRequest request = exchange.getRequest().mutate()
                .header(HEADER_NAME, orgId.trim())
                .build();
        return chain.filter(exchange.mutate().request(request).build());
    }
}
