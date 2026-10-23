package com.collabflow.platform.controller;

import com.collabflow.common.api.ApiResponse;
import com.collabflow.common.openapi.OpenApiSupport;
import com.collabflow.common.security.CurrentUserId;
import com.collabflow.common.web.ApiId;
import com.collabflow.platform.dto.response.AnnouncementResponse;
import com.collabflow.platform.service.AdminConsoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/announcements")
@Tag(name = "Announcements")
@SecurityRequirement(name = OpenApiSupport.API_KEY_SCHEME)
@SecurityRequirement(name = OpenApiSupport.BEARER_SCHEME)
public class AnnouncementController {

    private final AdminConsoleService adminConsoleService;

    public AnnouncementController(AdminConsoleService adminConsoleService) {
        this.adminConsoleService = adminConsoleService;
    }

    @GetMapping("/active")
    @ApiId("ANN-001")
    @Operation(summary = "Latest active announcement for the signed-in shell")
    public ApiResponse<AnnouncementResponse> active() {
        return ApiResponse.ok(adminConsoleService.activeAnnouncement());
    }
}
