package com.collabflow.gateway.filter;

import com.collabflow.gateway.config.GatewaySecurityProperties;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Edge check: every {@code /api/**} call through the gateway needs {@code X-API-Key}.
 * Downstream services still validate too (defense in depth).
 */
@Component
public class ApiKeyGatewayFilter implements WebFilter, Ordered {

    public static final String HEADER_NAME = "X-API-Key";

    private static final Logger log = LoggerFactory.getLogger(ApiKeyGatewayFilter.class);

    private final GatewaySecurityProperties properties;

    public ApiKeyGatewayFilter(GatewaySecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (path == null || !path.startsWith("/api/")) {
            return chain.filter(exchange);
        }

        String expected = properties.getSecurity().getApiKey();
        String provided = exchange.getRequest().getHeaders().getFirst(HEADER_NAME);
        // Browsers cannot set custom headers on WebSocket easily — allow ?apiKey= for WS only.
        if (!StringUtils.hasText(provided) && path.startsWith("/api/v1/realtime/ws")) {
            provided = exchange.getRequest().getQueryParams().getFirst("apiKey");
        }

        if (!StringUtils.hasText(expected) || !expected.equals(provided)) {
            log.warn("Gateway rejected {} {} — missing/invalid {}",
                    exchange.getRequest().getMethod(), path, HEADER_NAME);
            return unauthorized(exchange, path, "Invalid or missing X-API-Key");
        }

        return chain.filter(exchange);
    }

    private static Mono<Void> unauthorized(ServerWebExchange exchange, String path, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = """
                {"success":false,"message":"%s","errorCode":"UNAUTHORIZED","path":"%s"}
                """.formatted(message, path).trim();
        DataBuffer buffer = exchange.getResponse().bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
