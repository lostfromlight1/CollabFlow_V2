package com.collabflow.platform.controller;

import com.collabflow.common.api.ApiResponse;
import com.collabflow.common.openapi.OpenApiSupport;
import com.collabflow.common.web.ApiId;
import com.collabflow.platform.dto.response.FeatureFlagResponse;
import com.collabflow.platform.service.AdminConsoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/feature-flags")
@Tag(name = "Feature flags")
@SecurityRequirement(name = OpenApiSupport.API_KEY_SCHEME)
@SecurityRequirement(name = OpenApiSupport.BEARER_SCHEME)
public class FeatureFlagController {

    private final AdminConsoleService adminConsoleService;

    public FeatureFlagController(AdminConsoleService adminConsoleService) {
        this.adminConsoleService = adminConsoleService;
    }

    @GetMapping
    @ApiId("FLAG-001")
    @Operation(summary = "Enabled flags. A missing key means enabled")
    public ApiResponse<List<FeatureFlagResponse>> list() {
        return ApiResponse.ok(adminConsoleService.listPublicFlags());
    }
}
