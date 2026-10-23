package com.collabflow.platform.controller;

import com.collabflow.common.api.ApiResponse;
import com.collabflow.common.api.PageResponse;
import com.collabflow.common.openapi.OpenApiSupport;
import com.collabflow.common.security.CurrentUserId;
import com.collabflow.common.web.ApiId;
import com.collabflow.platform.dto.response.AdminAuditItem;
import com.collabflow.platform.service.AdminConsoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/audit")
@Tag(name = "Admin — audit")
@SecurityRequirement(name = OpenApiSupport.API_KEY_SCHEME)
@SecurityRequirement(name = OpenApiSupport.BEARER_SCHEME)
public class AdminAuditController {

    private final AdminConsoleService adminConsoleService;

    public AdminAuditController(AdminConsoleService adminConsoleService) {
        this.adminConsoleService = adminConsoleService;
    }

    @GetMapping
    @ApiId("ADMIN-AUDIT-001")
    @Operation(summary = "Filter the master-admin audit log")
    public ApiResponse<PageResponse<AdminAuditItem>> list(
            @CurrentUserId UUID adminId,
            @RequestParam(required = false) UUID actorUserId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) UUID targetId,
            @RequestParam(required = false) Instant createdFrom,
            @RequestParam(required = false) Instant createdTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(adminConsoleService.audit(
                adminId, actorUserId, action, targetType, targetId, createdFrom, createdTo, page, size
        ));
    }
}
