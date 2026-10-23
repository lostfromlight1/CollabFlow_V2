package com.collabflow.user.service;

import com.collabflow.common.api.PageResponse;
import com.collabflow.user.dto.request.AdminCreateUserRequest;
import com.collabflow.user.dto.request.SetMasterAdminRequest;
import com.collabflow.user.dto.request.UpdateProfileRequest;
import com.collabflow.user.dto.response.AdminUserResponse;
import com.collabflow.user.dto.response.UserResponse;
import java.time.Instant;
import java.util.UUID;

public interface UserService {

    UserResponse me(UUID userId);

    UserResponse updateProfile(UUID userId, UpdateProfileRequest request);

    UserResponse adminCreateUser(UUID adminUserId, AdminCreateUserRequest request);

    PageResponse<AdminUserResponse> adminList(
            UUID adminId,
            String q,
            Boolean active,
            Boolean masterAdmin,
            Boolean emailVerified,
            UUID organizationId,
            Instant createdFrom,
            Instant createdTo,
            String deleted,
            int page,
            int size,
            String sort
    );

    AdminUserResponse adminGet(UUID adminId, UUID userId);

    String adminExport(
            UUID adminId,
            String q,
            Boolean active,
            Boolean masterAdmin,
            Boolean emailVerified,
            UUID organizationId,
            Instant createdFrom,
            Instant createdTo,
            String deleted,
            String sort
    );

    void setMasterAdmin(UUID adminId, UUID userId, SetMasterAdminRequest request);

    void softDelete(UUID adminId, UUID userId);

    void restore(UUID adminId, UUID userId);
}
