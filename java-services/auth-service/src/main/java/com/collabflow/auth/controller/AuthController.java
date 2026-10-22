package com.collabflow.auth.controller;

import com.collabflow.auth.dto.request.ChangePasswordRequest;
import com.collabflow.auth.dto.request.ForgotPasswordRequest;
import com.collabflow.auth.dto.request.LoginRequest;
import com.collabflow.auth.dto.request.RefreshRequest;
import com.collabflow.auth.dto.request.RegisterRequest;
import com.collabflow.auth.dto.request.ResetPasswordRequest;
import com.collabflow.auth.dto.request.ResendVerificationRequest;
import com.collabflow.auth.dto.request.SelectOrganizationRequest;
import com.collabflow.auth.dto.request.UpdateProfileRequest;
import com.collabflow.auth.dto.request.VerifyEmailRequest;
import com.collabflow.auth.dto.response.AuthResponse;
import com.collabflow.auth.dto.response.OrganizationSummary;
import com.collabflow.auth.dto.response.SessionResponse;
import com.collabflow.auth.dto.response.UserResponse;
import com.collabflow.auth.service.AuthRateLimiter;
import com.collabflow.auth.service.AuthService;
import com.collabflow.common.api.ApiResponse;
import com.collabflow.common.openapi.OpenApiSupport;
import com.collabflow.common.security.CurrentUserId;
import com.collabflow.common.web.ApiId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
    @ApiId("AUTH-001")
    @Operation(summary = "Health check")
    public ApiResponse<HealthBody> health() {
        return ApiResponse.ok(new HealthBody("auth-service", "UP"));
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiId("AUTH-002")
    @Operation(summary = "Register (gated when public-register-enabled=false)")
    public ApiResponse<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest http
    ) {
        rateLimiter.checkRegister(clientKey(http));
        log.info("POST /register email={}", request.email());
        return ApiResponse.ok("Registered", authService.register(request));
    }

    @PostMapping("/login")
    @ApiId("AUTH-003")
    @Operation(summary = "Login (email + password; requires verified email)")
    public ApiResponse<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest http
    ) {
        rateLimiter.checkLogin(clientKey(http));
        log.info("POST /login email={}", request.email());
        return ApiResponse.ok("Logged in", authService.login(request));
    }

    @PostMapping("/refresh")
    @ApiId("AUTH-004")
    @Operation(summary = "Rotate refresh token and issue new access token")
    public ApiResponse<AuthResponse> refresh(
            @Valid @RequestBody RefreshRequest request,
            HttpServletRequest http
    ) {
        rateLimiter.checkRefresh(clientKey(http));
        return ApiResponse.ok("Token refreshed", authService.refresh(request));
    }

    @PostMapping("/logout")
    @ApiId("AUTH-005")
    @Operation(summary = "Revoke refresh token")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ApiResponse.okMessage("Logged out");
    }

    @GetMapping("/me")
    @ApiId("AUTH-006")
    @Operation(summary = "Current user profile")
    @SecurityRequirement(name = OpenApiSupport.BEARER_SCHEME)
    public ApiResponse<UserResponse> me(@CurrentUserId UUID userId) {
        return ApiResponse.ok(authService.me(userId));
    }

    @PostMapping("/select-organization")
    @ApiId("AUTH-016")
    @Operation(summary = "Select organization after multi-org login (re-issues tokens + selection)")
    @SecurityRequirement(name = OpenApiSupport.BEARER_SCHEME)
    public ApiResponse<AuthResponse> selectOrganization(
            @CurrentUserId UUID userId,
            @Valid @RequestBody SelectOrganizationRequest request
    ) {
        return ApiResponse.ok("Organization selected", authService.selectOrganization(userId, request));
    }

    @GetMapping("/organizations")
    @ApiId("AUTH-017")
    @Operation(summary = "List organizations for the current user (org picker)")
    @SecurityRequirement(name = OpenApiSupport.BEARER_SCHEME)
    public ApiResponse<List<OrganizationSummary>> myOrganizations(@CurrentUserId UUID userId) {
        return ApiResponse.ok(authService.listMyOrganizations(userId));
    }

    @PutMapping("/me")
    @ApiId("AUTH-007")
    @Operation(summary = "Update profile (name, avatarUrl)")
    @SecurityRequirement(name = OpenApiSupport.BEARER_SCHEME)
    public ApiResponse<UserResponse> updateMe(
            @CurrentUserId UUID userId,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ApiResponse.ok("Updated", authService.updateProfile(userId, request));
    }

    @PostMapping("/verify-email")
    @ApiId("AUTH-008")
    @Operation(summary = "Verify email with token from email.send")
    public ApiResponse<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request);
        return ApiResponse.okMessage("Email verified");
    }

    @PostMapping("/resend-verification")
    @ApiId("AUTH-009")
    @Operation(summary = "Resend verification email (rate-limited)")
    public ApiResponse<Void> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request,
            HttpServletRequest http
    ) {
        rateLimiter.checkResendVerify(clientKey(http));
        authService.resendVerification(request);
        return ApiResponse.okMessage("If the account exists and is unverified, a message was sent");
    }

    @PostMapping("/forgot-password")
    @ApiId("AUTH-010")
    @Operation(summary = "Request password reset email")
    public ApiResponse<Void> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest http
    ) {
        rateLimiter.checkForgotPassword(clientKey(http));
        authService.forgotPassword(request);
        return ApiResponse.okMessage("If the account exists, a reset message was sent");
    }

    @PostMapping("/reset-password")
    @ApiId("AUTH-011")
    @Operation(summary = "Reset password with token")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.okMessage("Password reset");
    }

    @PostMapping("/change-password")
    @ApiId("AUTH-012")
    @Operation(summary = "Change password (authenticated)")
    @SecurityRequirement(name = OpenApiSupport.BEARER_SCHEME)
    public ApiResponse<Void> changePassword(
            @CurrentUserId UUID userId,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        authService.changePassword(userId, request);
        return ApiResponse.okMessage("Password changed");
    }

    @GetMapping("/sessions")
    @ApiId("AUTH-013")
    @Operation(summary = "List active sessions (no raw tokens)")
    @SecurityRequirement(name = OpenApiSupport.BEARER_SCHEME)
    public ApiResponse<List<SessionResponse>> sessions(@CurrentUserId UUID userId) {
        return ApiResponse.ok(authService.listSessions(userId));
    }

    @PostMapping("/sessions/revoke-all")
    @ApiId("AUTH-014")
    @Operation(summary = "Revoke all refresh sessions")
    @SecurityRequirement(name = OpenApiSupport.BEARER_SCHEME)
    public ApiResponse<Void> revokeAllSessions(@CurrentUserId UUID userId) {
        authService.revokeAllSessions(userId);
        return ApiResponse.okMessage("All sessions revoked");
    }

    @PostMapping("/deactivate")
    @ApiId("AUTH-015")
    @Operation(summary = "Soft-deactivate current account")
    @SecurityRequirement(name = OpenApiSupport.BEARER_SCHEME)
    public ApiResponse<Void> deactivate(@CurrentUserId UUID userId) {
        authService.deactivate(userId);
        return ApiResponse.okMessage("Account deactivated");
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
