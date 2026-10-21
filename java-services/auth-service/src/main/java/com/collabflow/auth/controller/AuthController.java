package com.collabflow.auth.controller;

import com.collabflow.auth.dto.request.LoginRequest;
import com.collabflow.auth.dto.request.RefreshRequest;
import com.collabflow.auth.dto.request.RegisterRequest;
import com.collabflow.auth.dto.response.AuthResponse;
import com.collabflow.auth.dto.response.UserResponse;
import com.collabflow.auth.service.AuthRateLimiter;
import com.collabflow.auth.service.AuthService;
import com.collabflow.common.api.ApiResponse;
import com.collabflow.common.openapi.OpenApiSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Registration, login, tokens, current user")
@SecurityRequirement(name = OpenApiSupport.API_KEY_SCHEME)
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final AuthRateLimiter rateLimiter;

    public AuthController(AuthService authService, AuthRateLimiter rateLimiter) {
        this.authService = authService;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ApiResponse<HealthBody> health() {
        log.debug("GET /health");
        return ApiResponse.ok(new HealthBody("auth-service", "UP"));
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new user")
    public ApiResponse<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest http
    ) {
        rateLimiter.checkRegister(clientKey(http));
        log.info("POST /register email={}", request.email());
        return ApiResponse.ok("Registered", authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Login (email + password; organization scope later)")
    public ApiResponse<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest http
    ) {
        rateLimiter.checkLogin(clientKey(http));
        log.info("POST /login email={}", request.email());
        return ApiResponse.ok("Logged in", authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate refresh token and issue new access token")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        log.info("POST /refresh");
        return ApiResponse.ok("Token refreshed", authService.refresh(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke refresh token")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshRequest request) {
        log.info("POST /logout");
        authService.logout(request.refreshToken());
        return ApiResponse.okMessage("Logged out");
    }

    @GetMapping("/me")
    @Operation(summary = "Current user profile")
    @SecurityRequirement(name = OpenApiSupport.API_KEY_SCHEME)
    @SecurityRequirement(name = OpenApiSupport.BEARER_SCHEME)
    public ApiResponse<UserResponse> me(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        log.info("GET /me userId={}", userId);
        return ApiResponse.ok(authService.me(userId));
    }

    private static String clientKey(HttpServletRequest http) {
        String forwarded = http.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return http.getRemoteAddr() != null ? http.getRemoteAddr() : "unknown";
    }

    public record HealthBody(String service, String status) {}
}
