package com.collabflow.platform.controller;

import com.collabflow.common.api.ApiResponse;
import com.collabflow.common.api.PageResponse;
import com.collabflow.common.openapi.OpenApiSupport;
import com.collabflow.common.security.CurrentUserId;
import com.collabflow.common.web.ApiId;
import com.collabflow.platform.dto.request.AddMemberRequest;
import com.collabflow.platform.dto.request.AdminCreateOrganizationRequest;
import com.collabflow.platform.dto.request.TransferOwnershipRequest;
import com.collabflow.platform.dto.response.AdminOrganizationDetail;
import com.collabflow.platform.dto.response.AdminOrganizationSummary;
import com.collabflow.platform.dto.response.AdminUserMembership;
import com.collabflow.platform.dto.response.MemberResponse;
import com.collabflow.platform.dto.response.OrganizationResponse;
import com.collabflow.platform.service.AdminConsoleService;
import com.collabflow.platform.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    private final AdminConsoleService adminConsoleService;

    public AdminOrganizationController(
            OrganizationService organizationService,
            AdminConsoleService adminConsoleService
    ) {
        this.organizationService = organizationService;
        this.adminConsoleService = adminConsoleService;
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

    @GetMapping
    @ApiId("ADMIN-ORG-004")
    @Operation(summary = "List every organization")
    public ApiResponse<PageResponse<AdminOrganizationSummary>> list(
            @CurrentUserId UUID adminId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String plan,
            @RequestParam(required = false) Instant createdFrom,
            @RequestParam(required = false) Instant createdTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        return ApiResponse.ok(adminConsoleService.listOrganizations(
                adminId, q, active, plan, createdFrom, createdTo, page, size, sort
        ));
    }

    @GetMapping(value = "/export", produces = "text/csv")
    @ApiId("ADMIN-ORG-008")
    @Operation(summary = "Export organizations as CSV")
    public ResponseEntity<String> export(
            @CurrentUserId UUID adminId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String plan,
            @RequestParam(required = false) Instant createdFrom,
            @RequestParam(required = false) Instant createdTo,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        String csv = adminConsoleService.exportOrganizations(
                adminId, q, active, plan, createdFrom, createdTo, sort
        );
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"organizations.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping("/by-user/{userId}")
    @ApiId("ADMIN-ORG-009")
    @Operation(summary = "Organizations a user belongs to")
    public ApiResponse<List<AdminUserMembership>> byUser(
            @CurrentUserId UUID adminId,
            @PathVariable UUID userId
    ) {
        return ApiResponse.ok(adminConsoleService.membershipsForUser(adminId, userId));
    }

    @GetMapping("/{id}")
    @ApiId("ADMIN-ORG-005")
    @Operation(summary = "Organization detail, including inactive")
    public ApiResponse<AdminOrganizationDetail> get(@CurrentUserId UUID adminId, @PathVariable UUID id) {
        return ApiResponse.ok(adminConsoleService.getOrganization(adminId, id));
    }

    @GetMapping("/{id}/members")
    @ApiId("ADMIN-ORG-010")
    @Operation(summary = "Paged organization members")
    public ApiResponse<PageResponse<MemberResponse>> members(
            @CurrentUserId UUID adminId,
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(adminConsoleService.listMembers(adminId, id, page, size));
    }

    @PostMapping("/{id}/deactivate")
    @ApiId("ADMIN-ORG-006")
    @Operation(summary = "Deactivate an organization")
    public ApiResponse<Void> deactivate(@CurrentUserId UUID adminId, @PathVariable UUID id) {
        adminConsoleService.deactivateOrganization(adminId, id);
        return ApiResponse.okMessage("Organization deactivated");
    }

    @PostMapping("/{id}/activate")
    @ApiId("ADMIN-ORG-007")
    @Operation(summary = "Activate an organization")
    public ApiResponse<Void> activate(@CurrentUserId UUID adminId, @PathVariable UUID id) {
        adminConsoleService.activateOrganization(adminId, id);
        return ApiResponse.okMessage("Organization activated");
    }
}
