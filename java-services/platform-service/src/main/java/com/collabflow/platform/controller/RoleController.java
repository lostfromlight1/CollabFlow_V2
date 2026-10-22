package com.collabflow.platform.controller;

import com.collabflow.common.api.ApiResponse;
import com.collabflow.common.openapi.OpenApiSupport;
import com.collabflow.common.web.ApiId;
import com.collabflow.platform.dto.response.RoleResponse;
import com.collabflow.platform.service.OrganizationService;
import com.collabflow.platform.service.WorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/roles")
@Tag(name = "Roles")
@SecurityRequirement(name = OpenApiSupport.API_KEY_SCHEME)
@SecurityRequirement(name = OpenApiSupport.BEARER_SCHEME)
public class RoleController {

    private static final Logger log = LoggerFactory.getLogger(RoleController.class);

    private final OrganizationService organizationService;
    private final WorkspaceService workspaceService;

    public RoleController(OrganizationService organizationService, WorkspaceService workspaceService) {
        this.organizationService = organizationService;
        this.workspaceService = workspaceService;
    }

    @GetMapping
    @ApiId("ROLE-001")
    @Operation(summary = "List system roles by scope (ORGANIZATION | WORKSPACE)")
    public ApiResponse<List<RoleResponse>> list(@RequestParam(defaultValue = "ORGANIZATION") String scope) {
        log.info("GET /roles scope={}", scope);
        if ("WORKSPACE".equalsIgnoreCase(scope)) {
            return ApiResponse.ok(workspaceService.listWorkspaceRoles());
        }
        if ("ORGANIZATION".equalsIgnoreCase(scope)) {
            return ApiResponse.ok(organizationService.listOrganizationRoles());
        }
        return ApiResponse.ok(List.of());
    }
}
