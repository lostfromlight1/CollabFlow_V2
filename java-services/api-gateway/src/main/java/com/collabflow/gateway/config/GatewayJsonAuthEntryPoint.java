package com.collabflow.gateway.config;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 401 JSON matching {@code common} ErrorResponse shape (gateway is WebFlux — cannot use servlet handler).
 */
@Component
public class GatewayJsonAuthEntryPoint implements ServerAuthenticationEntryPoint {

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
        return write(exchange, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required");
    }

    static Mono<Void> write(ServerWebExchange exchange, HttpStatus status, String code, String message) {
        String path = exchange.getRequest().getURI().getPath();
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = """
                {"success":false,"message":"%s","errorCode":"%s","path":"%s","timestamp":"%s"}
                """.formatted(message, code, path, Instant.now()).trim();
        DataBuffer buffer = exchange.getResponse().bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
