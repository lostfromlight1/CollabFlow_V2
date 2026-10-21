package com.collabflow.gateway.config;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Gateway edge security.
 *
 * <ul>
 *   <li>Public auth endpoints: register / login / refresh / logout / health</li>
 *   <li>Protected: {@code GET /api/v1/auth/me} needs Bearer JWT (same secret as auth-service)</li>
 *   <li>Platform/task routes: permit for now (stubs); tighten when those APIs ship</li>
 * </ul>
 *
 * <p>Why JWT at the gateway? Reject bad tokens before they hit every service.
 * Auth-service still validates again for direct (non-gateway) calls.
 */
@Configuration
@EnableWebFluxSecurity
@EnableConfigurationProperties(GatewaySecurityProperties.class)
public class GatewaySecurityConfig {

    @Bean
    ReactiveJwtDecoder reactiveJwtDecoder(GatewaySecurityProperties properties) {
        byte[] secret = properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        SecretKey key = new SecretKeySpec(secret, "HmacSHA256");
        return NimbusReactiveJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(ex -> ex
                        .pathMatchers("/actuator/**").permitAll()
                        .pathMatchers(
                                "/api/v1/auth/health",
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/logout"
                        ).permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/v1/auth/me").authenticated()
                        // Stubs until platform/task Week 5+
                        .pathMatchers(
                                "/api/v1/platform/**",
                                "/api/v1/organizations/**",
                                "/api/v1/workspaces/**",
                                "/api/v1/projects/**",
                                "/api/v1/tasks/**",
                                "/api/v1/workflows/**",
                                "/api/v1/sprints/**"
                        ).permitAll()
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}));
        return http.build();
    }
}
