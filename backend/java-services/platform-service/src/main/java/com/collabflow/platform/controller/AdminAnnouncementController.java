package com.collabflow.platform.controller;

import com.collabflow.common.api.ApiResponse;
import com.collabflow.common.openapi.OpenApiSupport;
import com.collabflow.common.security.CurrentUserId;
import com.collabflow.common.web.ApiId;
import com.collabflow.platform.dto.request.AnnouncementRequest;
import com.collabflow.platform.dto.response.AnnouncementResponse;
import com.collabflow.platform.service.AdminConsoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
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
@RequestMapping("/api/v1/admin/announcements")
@Tag(name = "Admin — announcements")
@SecurityRequirement(name = OpenApiSupport.API_KEY_SCHEME)
@SecurityRequirement(name = OpenApiSupport.BEARER_SCHEME)
public class AdminAnnouncementController {

    private final AdminConsoleService adminConsoleService;

    public AdminAnnouncementController(AdminConsoleService adminConsoleService) {
        this.adminConsoleService = adminConsoleService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @ApiId("ADMIN-ANN-001")
    @Operation(summary = "Create an announcement")
    public ApiResponse<AnnouncementResponse> create(
            @CurrentUserId UUID adminId,
            @Valid @RequestBody AnnouncementRequest request
    ) {
        return ApiResponse.ok("Announcement created", adminConsoleService.createAnnouncement(adminId, request));
    }

    @GetMapping
    @ApiId("ADMIN-ANN-002")
    @Operation(summary = "List announcements")
    public ApiResponse<List<AnnouncementResponse>> list(@CurrentUserId UUID adminId) {
        return ApiResponse.ok(adminConsoleService.listAnnouncements(adminId));
    }

    @PutMapping("/{id}")
    @ApiId("ADMIN-ANN-003")
    @Operation(summary = "Update an announcement")
    public ApiResponse<AnnouncementResponse> update(
            @CurrentUserId UUID adminId,
            @PathVariable UUID id,
            @Valid @RequestBody AnnouncementRequest request
    ) {
        return ApiResponse.ok("Announcement updated", adminConsoleService.updateAnnouncement(adminId, id, request));
    }

    @DeleteMapping("/{id}")
    @ApiId("ADMIN-ANN-004")
    @Operation(summary = "Delete an announcement")
    public ApiResponse<Void> delete(@CurrentUserId UUID adminId, @PathVariable UUID id) {
        adminConsoleService.deleteAnnouncement(adminId, id);
        return ApiResponse.okMessage("Announcement deleted");
    }
}
