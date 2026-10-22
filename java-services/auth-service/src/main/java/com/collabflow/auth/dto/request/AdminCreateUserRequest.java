package com.collabflow.auth.dto.request;

/**
 * DEPRECATED (Wave 3): use {@code com.collabflow.user.dto.request.AdminCreateUserRequest}
 * on user-service ({@code USER-020}).
 */
@Deprecated
public record AdminCreateUserRequest(
        String email,
        String temporaryPassword,
        String name
) {}
