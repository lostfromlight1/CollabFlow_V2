package com.collabflow.user.service;

import com.collabflow.user.dto.request.AdminCreateUserRequest;
import com.collabflow.user.dto.request.UpdateProfileRequest;
import com.collabflow.user.dto.response.UserResponse;
import java.util.UUID;

public interface UserService {

    UserResponse me(UUID userId);

    UserResponse updateProfile(UUID userId, UpdateProfileRequest request);

    UserResponse adminCreateUser(UUID adminUserId, AdminCreateUserRequest request);
}
