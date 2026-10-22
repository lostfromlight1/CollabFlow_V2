package com.collabflow.user.controller;

import com.collabflow.common.api.ApiResponse;
import com.collabflow.common.openapi.OpenApiSupport;
import com.collabflow.common.security.CurrentUserId;
import com.collabflow.common.web.ApiId;
import com.collabflow.user.dto.request.UpdateProfileRequest;
import com.collabflow.user.dto.response.UserResponse;
import com.collabflow.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users")
@SecurityRequirement(name = OpenApiSupport.API_KEY_SCHEME)
@SecurityRequirement(name = OpenApiSupport.BEARER_SCHEME)
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @ApiId("USER-001")
    @Operation(summary = "Current user profile")
    public ApiResponse<UserResponse> me(@CurrentUserId UUID userId) {
        log.debug("GET /users/me userId={}", userId);
        return ApiResponse.ok(userService.me(userId));
    }

    @PutMapping("/me")
    @ApiId("USER-002")
    @Operation(summary = "Update current user profile")
    public ApiResponse<UserResponse> updateMe(
            @CurrentUserId UUID userId,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        log.info("PUT /users/me userId={}", userId);
        return ApiResponse.ok("Updated", userService.updateProfile(userId, request));
    }
}
