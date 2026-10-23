package com.collabflow.auth.controller;

import com.collabflow.auth.config.JwtService;
import com.collabflow.auth.dto.request.AdminResetPasswordRequest;
import com.collabflow.auth.dto.request.ImpersonateRequest;
import com.collabflow.auth.dto.response.ImpersonationResponse;
import com.collabflow.auth.service.AuthService;
import com.collabflow.common.api.ApiResponse;
import com.collabflow.common.exception.ForbiddenException;
import com.collabflow.common.openapi.OpenApiSupport;
import com.collabflow.common.security.CurrentUserId;
import com.collabflow.common.web.ApiId;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin — sessions")
@SecurityRequirement(name = OpenApiSupport.API_KEY_SCHEME)
@SecurityRequirement(name = OpenApiSupport.BEARER_SCHEME)
public class AdminSessionController {

    private static final Logger log = LoggerFactory.getLogger(AdminSessionController.class);

    private final AuthService authService;
    private final JwtService jwtService;

    public AdminSessionController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/users/{id}/deactivate")
    @ApiId("AUTH-021")
    @Operation(summary = "Deactivate a user and revoke their sessions")
    public ApiResponse<Void> deactivate(@CurrentUserId UUID adminId, @PathVariable UUID id) {
        log.info("POST /admin/users/{}/deactivate admin={}", id, adminId);
        authService.adminDeactivate(adminId, id);
        return ApiResponse.okMessage("User deactivated");
    }

    @PostMapping("/users/{id}/activate")
    @ApiId("AUTH-022")
    @Operation(summary = "Activate a user")
    public ApiResponse<Void> activate(@CurrentUserId UUID adminId, @PathVariable UUID id) {
        log.info("POST /admin/users/{}/activate admin={}", id, adminId);
        authService.adminActivate(adminId, id);
        return ApiResponse.okMessage("User activated");
    }

    @PostMapping("/users/{id}/sessions/revoke")
    @ApiId("AUTH-023")
    @Operation(summary = "Revoke a user's refresh tokens")
    public ApiResponse<Void> revoke(@CurrentUserId UUID adminId, @PathVariable UUID id) {
        log.info("POST /admin/users/{}/sessions/revoke admin={}", id, adminId);
        authService.adminRevokeSessions(adminId, id);
        return ApiResponse.okMessage("Sessions revoked");
    }

    @PostMapping("/users/{id}/reset-password")
    @ApiId("AUTH-024")
    @Operation(summary = "Set a temporary password and revoke sessions")
    public ApiResponse<Void> resetPassword(
            @CurrentUserId UUID adminId,
            @PathVariable UUID id,
            @Valid @RequestBody AdminResetPasswordRequest request
    ) {
        log.info("POST /admin/users/{}/reset-password admin={}", id, adminId);
        authService.adminResetPassword(adminId, id, request);
        return ApiResponse.okMessage("Password reset");
    }

    @PostMapping("/users/{id}/resend-verification")
    @ApiId("AUTH-025")
    @Operation(summary = "Resend the verification email")
    public ApiResponse<Void> resend(@CurrentUserId UUID adminId, @PathVariable UUID id) {
        log.info("POST /admin/users/{}/resend-verification admin={}", id, adminId);
        return ApiResponse.okMessage(authService.adminResendVerification(adminId, id));
    }

    @PostMapping("/sessions/impersonate")
    @ApiId("AUTH-026")
    @Operation(summary = "Issue a 15-minute read-only token for a user")
    public ApiResponse<ImpersonationResponse> impersonate(
            @CurrentUserId UUID adminId,
            @Valid @RequestBody ImpersonateRequest request
    ) {
        log.info("POST /admin/sessions/impersonate admin={} target={}", adminId, request.userId());
        return ApiResponse.ok("Impersonation started", authService.impersonate(adminId, request));
    }

    @PostMapping("/sessions/impersonate/end")
    @ApiId("AUTH-027")
    @Operation(summary = "Record the end of a read-only impersonation")
    public ApiResponse<Void> endImpersonation(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new ForbiddenException("Impersonation token required");
        }
        Claims claims = jwtService.parse(header.substring("Bearer ".length()).trim());
        Object actor = claims.get("actor");
        if (actor == null || actor.toString().isBlank()) {
            throw new ForbiddenException("Impersonation token required");
        }
        UUID actorId = UUID.fromString(actor.toString());
        UUID targetId = UUID.fromString(claims.getSubject());
        log.info("POST /admin/sessions/impersonate/end actor={} target={}", actorId, targetId);
        authService.endImpersonation(actorId, targetId);
        return ApiResponse.okMessage("Impersonation ended");
    }
}
