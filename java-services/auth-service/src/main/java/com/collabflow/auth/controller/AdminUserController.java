package com.collabflow.auth.controller;

/**
 * DEPRECATED (Wave 3): Admin user CRUD moved to {@code user-service}.
 *
 * <p>Gateway routes {@code POST /api/v1/admin/users} → user-service:8084 ({@code USER-020}).
 * See {@code com.collabflow.user.controller.AdminUserController}.
 *
 * <p>This class is intentionally not a {@code @RestController} so auth no longer serves the route.
 */
public final class AdminUserController {

    private AdminUserController() {}
}
