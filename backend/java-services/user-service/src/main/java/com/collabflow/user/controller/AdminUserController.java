package com.collabflow.user.controller;

import com.collabflow.common.api.ApiResponse;
import com.collabflow.common.api.PageResponse;
import com.collabflow.common.openapi.OpenApiSupport;
import com.collabflow.common.security.CurrentUserId;
import com.collabflow.common.web.ApiId;
import com.collabflow.user.dto.request.AdminCreateUserRequest;
import com.collabflow.user.dto.request.SetMasterAdminRequest;
import com.collabflow.user.dto.response.AdminUserResponse;
import com.collabflow.user.dto.response.UserResponse;
import com.collabflow.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@Tag(name = "Admin Users")
@SecurityRequirement(name = OpenApiSupport.API_KEY_SCHEME)
@SecurityRequirement(name = OpenApiSupport.BEARER_SCHEME)
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @ApiId("USER-020")
    @Operation(summary = "Master admin creates a user")
    public ApiResponse<UserResponse> create(
            @CurrentUserId UUID adminId,
            @Valid @RequestBody AdminCreateUserRequest request
    ) {
        return ApiResponse.ok("User created", userService.adminCreateUser(adminId, request));
    }

    @GetMapping
    @ApiId("USER-021")
    @Operation(summary = "List users")
    public ApiResponse<PageResponse<AdminUserResponse>> list(
            @CurrentUserId UUID adminId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean masterAdmin,
            @RequestParam(required = false) Boolean emailVerified,
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) Instant createdFrom,
            @RequestParam(required = false) Instant createdTo,
            @RequestParam(defaultValue = "false") String deleted,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        return ApiResponse.ok(userService.adminList(
                adminId, q, active, masterAdmin, emailVerified, organizationId,
                createdFrom, createdTo, deleted, page, size, sort
        ));
    }

    @GetMapping(value = "/export", produces = "text/csv")
    @ApiId("USER-025")
    @Operation(summary = "Export users as CSV")
    public ResponseEntity<String> export(
            @CurrentUserId UUID adminId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean masterAdmin,
            @RequestParam(required = false) Boolean emailVerified,
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) Instant createdFrom,
            @RequestParam(required = false) Instant createdTo,
            @RequestParam(defaultValue = "false") String deleted,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        String csv = userService.adminExport(
                adminId, q, active, masterAdmin, emailVerified, organizationId,
                createdFrom, createdTo, deleted, sort
        );
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"users.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping("/{id}")
    @ApiId("USER-022")
    @Operation(summary = "Get one user, including deleted")
    public ApiResponse<AdminUserResponse> get(@CurrentUserId UUID adminId, @PathVariable UUID id) {
        return ApiResponse.ok(userService.adminGet(adminId, id));
    }

    @PutMapping("/{id}/master-admin")
    @ApiId("USER-023")
    @Operation(summary = "Grant or revoke master admin")
    public ApiResponse<Void> masterAdmin(
            @CurrentUserId UUID adminId,
            @PathVariable UUID id,
            @Valid @RequestBody SetMasterAdminRequest request
    ) {
        userService.setMasterAdmin(adminId, id, request);
        return ApiResponse.okMessage("Master admin updated");
    }

    @PostMapping("/{id}/delete")
    @ApiId("USER-024")
    @Operation(summary = "Soft-delete a user")
    public ApiResponse<Void> delete(@CurrentUserId UUID adminId, @PathVariable UUID id) {
        userService.softDelete(adminId, id);
        return ApiResponse.okMessage("User deleted");
    }

    @PostMapping("/{id}/restore")
    @ApiId("USER-024")
    @Operation(summary = "Restore a soft-deleted user")
    public ApiResponse<Void> restore(@CurrentUserId UUID adminId, @PathVariable UUID id) {
        userService.restore(adminId, id);
        return ApiResponse.okMessage("User restored");
    }
}
