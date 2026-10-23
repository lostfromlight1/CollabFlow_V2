package com.collabflow.platform.controller;

import com.collabflow.common.api.ApiResponse;
import com.collabflow.common.openapi.OpenApiSupport;
import com.collabflow.common.security.CurrentUserId;
import com.collabflow.common.web.ApiId;
import com.collabflow.platform.dto.response.AdminStatsResponse;
import com.collabflow.platform.service.AdminConsoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/stats")
@Tag(name = "Admin — stats")
@SecurityRequirement(name = OpenApiSupport.API_KEY_SCHEME)
@SecurityRequirement(name = OpenApiSupport.BEARER_SCHEME)
public class AdminStatsController {

    private final AdminConsoleService adminConsoleService;

    public AdminStatsController(AdminConsoleService adminConsoleService) {
        this.adminConsoleService = adminConsoleService;
    }

    @GetMapping
    @ApiId("ADMIN-STATS-001")
    @Operation(summary = "Platform counts and signup trend")
    public ApiResponse<AdminStatsResponse> stats(@CurrentUserId UUID adminId) {
        return ApiResponse.ok(adminConsoleService.stats(adminId));
    }
}
