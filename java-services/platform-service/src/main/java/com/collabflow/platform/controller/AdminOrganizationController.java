package com.collabflow.platform.controller;

import com.collabflow.common.api.ApiResponse;
import com.collabflow.common.openapi.OpenApiSupport;
import com.collabflow.common.security.CurrentUserId;
import com.collabflow.common.web.ApiId;
import com.collabflow.platform.dto.request.AddMemberRequest;
import com.collabflow.platform.dto.request.AdminCreateOrganizationRequest;
import com.collabflow.platform.dto.request.TransferOwnershipRequest;
import com.collabflow.platform.dto.response.MemberResponse;
import com.collabflow.platform.dto.response.OrganizationResponse;
import com.collabflow.platform.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/organizations")
@Tag(name = "Admin — Organizations")
@SecurityRequirement(name = OpenApiSupport.API_KEY_SCHEME)
@SecurityRequirement(name = OpenApiSupport.BEARER_SCHEME)
public class AdminOrganizationController {

    private static final Logger log = LoggerFactory.getLogger(AdminOrganizationController.class);

    private final OrganizationService organizationService;

    public AdminOrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @ApiId("ADMIN-ORG-001")
    @Operation(summary = "Create organization as master admin (specify owner)")
    public ApiResponse<OrganizationResponse> create(
            @Valid @RequestBody AdminCreateOrganizationRequest request,
            @CurrentUserId UUID adminId
    ) {
        log.info("POST /admin/organizations slug={} adminId={} owner={}",
                request.slug(), adminId, request.ownerUserId());
        return ApiResponse.ok("Organization created", organizationService.createAsAdmin(adminId, request));
    }

    @PostMapping("/{id}/members")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiId("ADMIN-ORG-002")
    @Operation(summary = "Attach user to organization (master admin)")
    public ApiResponse<MemberResponse> attachMember(
            @PathVariable UUID id,
            @Valid @RequestBody AddMemberRequest request,
            @CurrentUserId UUID adminId
    ) {
        log.info("POST /admin/organizations/{}/members adminId={} newUser={}", id, adminId, request.userId());
        return ApiResponse.ok("Member attached", organizationService.addMemberAsAdmin(id, adminId, request));
    }

    @PostMapping("/{id}/transfer-ownership")
    @ApiId("ADMIN-ORG-003")
    @Operation(summary = "Transfer organization ownership (master admin)")
    public ApiResponse<OrganizationResponse> transferOwnership(
            @PathVariable UUID id,
            @Valid @RequestBody TransferOwnershipRequest request,
            @CurrentUserId UUID adminId
    ) {
        log.info("POST /admin/organizations/{}/transfer-ownership adminId={} newOwner={}",
                id, adminId, request.newOwnerUserId());
        return ApiResponse.ok(
                "Ownership transferred",
                organizationService.transferOwnershipAsAdmin(id, adminId, request)
        );
    }
}
