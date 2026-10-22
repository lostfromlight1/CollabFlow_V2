package com.collabflow.gateway.config;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** 403 JSON matching {@code common} ErrorResponse shape. */
@Component
public class GatewayJsonAccessDeniedHandler implements ServerAccessDeniedHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException denied) {
        return GatewayJsonAuthEntryPoint.write(
                exchange,
                HttpStatus.FORBIDDEN,
                "FORBIDDEN",
                "Access denied"
        );
    }
}
