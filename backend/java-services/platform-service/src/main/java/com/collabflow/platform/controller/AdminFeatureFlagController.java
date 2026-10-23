package com.collabflow.platform.controller;

import com.collabflow.common.api.ApiResponse;
import com.collabflow.common.openapi.OpenApiSupport;
import com.collabflow.common.security.CurrentUserId;
import com.collabflow.common.web.ApiId;
import com.collabflow.platform.dto.request.FeatureFlagRequest;
import com.collabflow.platform.dto.response.FeatureFlagResponse;
import com.collabflow.platform.service.AdminConsoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/feature-flags")
@Tag(name = "Admin — feature flags")
@SecurityRequirement(name = OpenApiSupport.API_KEY_SCHEME)
@SecurityRequirement(name = OpenApiSupport.BEARER_SCHEME)
public class AdminFeatureFlagController {

    private final AdminConsoleService adminConsoleService;

    public AdminFeatureFlagController(AdminConsoleService adminConsoleService) {
        this.adminConsoleService = adminConsoleService;
    }

    @GetMapping
    @ApiId("ADMIN-FLAG-001")
    @Operation(summary = "List feature flags")
    public ApiResponse<List<FeatureFlagResponse>> list(@CurrentUserId UUID adminId) {
        return ApiResponse.ok(adminConsoleService.listFlags(adminId));
    }

    @PutMapping("/{key}")
    @ApiId("ADMIN-FLAG-002")
    @Operation(summary = "Create or update a feature flag")
    public ApiResponse<FeatureFlagResponse> upsert(
            @CurrentUserId UUID adminId,
            @PathVariable String key,
            @Valid @RequestBody FeatureFlagRequest request
    ) {
        return ApiResponse.ok("Feature flag saved", adminConsoleService.upsertFlag(adminId, key, request));
    }

    @DeleteMapping("/{key}")
    @ApiId("ADMIN-FLAG-003")
    @Operation(summary = "Delete a feature flag")
    public ApiResponse<Void> delete(@CurrentUserId UUID adminId, @PathVariable String key) {
        adminConsoleService.deleteFlag(adminId, key);
        return ApiResponse.okMessage("Feature flag deleted");
    }
}
