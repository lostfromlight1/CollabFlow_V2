package com.collabflow.platform.controller;

import com.collabflow.common.api.ApiResponse;
import com.collabflow.common.openapi.OpenApiSupport;
import com.collabflow.common.security.CurrentUserId;
import com.collabflow.common.web.ApiId;
import com.collabflow.platform.dto.request.AddMemberRequest;
import com.collabflow.platform.dto.request.CreateOrganizationRequest;
import com.collabflow.platform.dto.request.UpdateMemberRoleRequest;
import com.collabflow.platform.dto.request.UpdateOrganizationRequest;
import com.collabflow.platform.dto.response.MemberResponse;
import com.collabflow.platform.dto.response.OrganizationResponse;
import com.collabflow.platform.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizations")
@Tag(name = "Organizations")
@SecurityRequirement(name = OpenApiSupport.API_KEY_SCHEME)
@SecurityRequirement(name = OpenApiSupport.BEARER_SCHEME)
public class OrganizationController {

    private static final Logger log = LoggerFactory.getLogger(OrganizationController.class);

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @ApiId("ORG-001")
    @Operation(summary = "Create organization (caller becomes Owner)")
    public ApiResponse<OrganizationResponse> create(
            @Valid @RequestBody CreateOrganizationRequest request,
            @CurrentUserId UUID userId
    ) {
        log.info("POST /organizations slug={} userId={}", request.slug(), userId);
        return ApiResponse.ok("Organization created", organizationService.create(userId, request));
    }

    @GetMapping
    @ApiId("ORG-002")
    @Operation(summary = "List my organizations")
    public ApiResponse<List<OrganizationResponse>> listMine(@CurrentUserId UUID userId) {
        log.info("GET /organizations userId={}", userId);
        return ApiResponse.ok(organizationService.listMine(userId));
    }

    @GetMapping("/{id}")
    @ApiId("ORG-003")
    @Operation(summary = "Get organization")
    public ApiResponse<OrganizationResponse> get(@PathVariable UUID id, @CurrentUserId UUID userId) {
        log.info("GET /organizations/{} userId={}", id, userId);
        return ApiResponse.ok(organizationService.get(id, userId));
    }

    @PutMapping("/{id}")
    @ApiId("ORG-004")
    @Operation(summary = "Update organization")
    public ApiResponse<OrganizationResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOrganizationRequest request,
            @CurrentUserId UUID userId
    ) {
        log.info("PUT /organizations/{} userId={}", id, userId);
        return ApiResponse.ok("Updated", organizationService.update(id, userId, request));
    }

    @DeleteMapping("/{id}")
    @ApiId("ORG-005")
    @Operation(summary = "Soft-delete organization")
    public ApiResponse<Void> delete(@PathVariable UUID id, @CurrentUserId UUID userId) {
        log.info("DELETE /organizations/{} userId={}", id, userId);
        organizationService.softDelete(id, userId);
        return ApiResponse.okMessage("Organization deactivated");
    }

    @GetMapping("/{id}/members")
    @ApiId("ORG-006")
    @Operation(summary = "List members")
    public ApiResponse<List<MemberResponse>> listMembers(@PathVariable UUID id, @CurrentUserId UUID userId) {
        log.info("GET /organizations/{}/members userId={}", id, userId);
        return ApiResponse.ok(organizationService.listMembers(id, userId));
    }

    @PostMapping("/{id}/members")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiId("ORG-007")
    @Operation(summary = "Add member")
    public ApiResponse<MemberResponse> addMember(
            @PathVariable UUID id,
            @Valid @RequestBody AddMemberRequest request,
            @CurrentUserId UUID userId
    ) {
        log.info("POST /organizations/{}/members userId={} newUser={}", id, userId, request.userId());
        return ApiResponse.ok("Member added", organizationService.addMember(id, userId, request));
    }

    @PutMapping("/{id}/members/{memberId}")
    @ApiId("ORG-008")
    @Operation(summary = "Update member role")
    public ApiResponse<MemberResponse> updateMember(
            @PathVariable UUID id,
            @PathVariable UUID memberId,
            @Valid @RequestBody UpdateMemberRoleRequest request,
            @CurrentUserId UUID userId
    ) {
        log.info("PUT /organizations/{}/members/{} userId={}", id, memberId, userId);
        return ApiResponse.ok("Role updated", organizationService.updateMemberRole(id, memberId, userId, request));
    }

    @DeleteMapping("/{id}/members/{memberId}")
    @ApiId("ORG-009")
    @Operation(summary = "Remove member")
    public ApiResponse<Void> removeMember(
            @PathVariable UUID id,
            @PathVariable UUID memberId,
            @CurrentUserId UUID userId
    ) {
        log.info("DELETE /organizations/{}/members/{} userId={}", id, memberId, userId);
        organizationService.removeMember(id, memberId, userId);
        return ApiResponse.okMessage("Member removed");
    }
}
