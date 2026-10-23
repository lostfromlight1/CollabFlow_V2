package com.collabflow.gateway.config;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Edge security: public auth routes; JWT for platform, tasks, collab, realtime.
 * Sprints require JWT (Wave 0 — was incorrectly permitAll).
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
    SecurityWebFilterChain springSecurityFilterChain(
            ServerHttpSecurity http,
            GatewayJsonAuthEntryPoint authEntryPoint,
            GatewayJsonAccessDeniedHandler accessDeniedHandler,
            Environment environment
    ) {
        boolean isDev = ArraysContainsDev(environment.getActiveProfiles());

        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeExchange(ex -> {
                    // Actuator: health always; gateway endpoint only in dev
                    ex.pathMatchers("/actuator/health", "/actuator/info").permitAll();
                    if (isDev) {
                        ex.pathMatchers("/actuator/**").permitAll();
                    } else {
                        ex.pathMatchers("/actuator/**").authenticated();
                    }
                    ex.pathMatchers(
                                    "/api/v1/auth/health",
                                    "/api/v1/auth/register",
                                    "/api/v1/auth/login",
                                    "/api/v1/auth/refresh",
                                    "/api/v1/auth/logout",
                                    // Wave 1 public identity paths (handlers land with auth Wave 1)
                                    "/api/v1/auth/verify-email",
                                    "/api/v1/auth/resend-verification",
                                    "/api/v1/auth/forgot-password",
                                    "/api/v1/auth/reset-password"
                            ).permitAll()
                            .pathMatchers("/api/v1/platform/health").permitAll()
                            .pathMatchers("/api/v1/users/health").permitAll()
                            .pathMatchers("/api/v1/tasks/health").permitAll()
                            .pathMatchers("/api/v1/collab/health").permitAll()
                            .pathMatchers("/api/v1/realtime/health").permitAll()
                            .pathMatchers("/api/v1/documents/health").permitAll()
                            // WS auth checked inside realtime-service (?token= or Authorization)
                            .pathMatchers("/api/v1/realtime/ws").permitAll()
                            // Invite landing page: peek by token (accept still requires JWT)
                            .pathMatchers(HttpMethod.GET, "/api/v1/invitations/token/**").permitAll()
                            .pathMatchers(HttpMethod.GET, "/api/v1/auth/me").authenticated()
                            .pathMatchers(
                                    "/api/v1/organizations/**",
                                    "/api/v1/workspaces/**",
                                    "/api/v1/projects/**",
                                    "/api/v1/roles/**",
                                    "/api/v1/tasks/**",
                                    "/api/v1/workflows/**",
                                    "/api/v1/task-types/**",
                                    "/api/v1/sprints/**",
                                    "/api/v1/notifications/**",
                                    "/api/v1/invitations/**",
                                    "/api/v1/comments/**",
                                    "/api/v1/activity/**",
                                    "/api/v1/realtime/**",
                                    "/api/v1/documents/**",
                                    "/api/v1/admin/**",
                                    "/api/v1/users/**"
                            ).authenticated()
                            .anyExchange().authenticated();
                })
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}));
        return http.build();
    }

    private static boolean ArraysContainsDev(String[] profiles) {
        if (profiles == null) {
            return true; // no profile → treat as local/dev for actuator convenience
        }
        for (String p : profiles) {
            if ("dev".equalsIgnoreCase(p) || "local".equalsIgnoreCase(p)) {
                return true;
            }
        }
        return profiles.length == 0;
    }
}
