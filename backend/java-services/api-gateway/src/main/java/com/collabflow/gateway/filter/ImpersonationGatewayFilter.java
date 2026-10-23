package com.collabflow.gateway.filter;

import java.nio.charset.StandardCharsets;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Blocks mutations while a master admin is impersonating a user.
 * The access token carries {@code actor} = admin id and {@code sub} = target id.
 */
@Component
public class ImpersonationGatewayFilter implements WebFilter, Ordered {

    private final ReactiveJwtDecoder jwtDecoder;

    public ImpersonationGatewayFilter(ReactiveJwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            return chain.filter(exchange);
        }
        String token = header.substring("Bearer ".length()).trim();
        if (token.isEmpty()) {
            return chain.filter(exchange);
        }
        return jwtDecoder.decode(token)
                .flatMap(jwt -> {
                    Object actor = jwt.getClaims().get("actor");
                    boolean present = actor != null && !actor.toString().isBlank();
                    HttpMethod method = exchange.getRequest().getMethod();
                    String path = exchange.getRequest().getURI().getPath();
                    String upgrade = exchange.getRequest().getHeaders().getFirst(HttpHeaders.UPGRADE);
                    if (ImpersonationRules.allowed(method, path, upgrade, present)) {
                        return chain.filter(exchange);
                    }
                    return forbidden(exchange);
                })
                .onErrorResume(ex -> chain.filter(exchange));
    }

    private static Mono<Void> forbidden(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = """
                {"success":false,"message":"Read-only impersonation","errorCode":"FORBIDDEN"}
                """.trim();
        DataBuffer buffer = exchange.getResponse().bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
