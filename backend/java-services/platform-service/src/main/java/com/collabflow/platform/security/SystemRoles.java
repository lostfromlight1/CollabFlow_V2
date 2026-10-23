package com.collabflow.platform.security;

import java.util.UUID;

/** Stable IDs from platform Flyway seeds. */
public final class SystemRoles {

    public static final UUID ORGANIZATION_OWNER = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    public static final UUID ORGANIZATION_ADMIN = UUID.fromString("a0000000-0000-4000-8000-000000000002");
    public static final UUID ORGANIZATION_MEMBER = UUID.fromString("a0000000-0000-4000-8000-000000000003");

    public static final UUID WORKSPACE_ADMIN = UUID.fromString("a0000000-0000-4000-8000-000000000004");
    public static final UUID WORKSPACE_MEMBER = UUID.fromString("a0000000-0000-4000-8000-000000000005");
    public static final UUID WORKSPACE_VIEWER = UUID.fromString("a0000000-0000-4000-8000-000000000006");

    public static final UUID PROJECT_ADMIN = UUID.fromString("a0000000-0000-4000-8000-000000000007");
    public static final UUID PROJECT_MEMBER = UUID.fromString("a0000000-0000-4000-8000-000000000008");
    public static final UUID PROJECT_VIEWER = UUID.fromString("a0000000-0000-4000-8000-000000000009");

    public static final String ORG_READ = "organization.read";
    public static final String ORG_UPDATE = "organization.update";
    public static final String ORG_DELETE = "organization.delete";
    public static final String ORG_MANAGE_MEMBERS = "organization.manage_members";

    public static final String WORKSPACE_CREATE = "workspace.create";
    public static final String WORKSPACE_READ = "workspace.read";
    public static final String WORKSPACE_UPDATE = "workspace.update";
    public static final String WORKSPACE_DELETE = "workspace.delete";
    public static final String WORKSPACE_MANAGE_MEMBERS = "workspace.manage_members";

    public static final String PROJECT_CREATE = "project.create";
    public static final String PROJECT_READ = "project.read";
    public static final String PROJECT_UPDATE = "project.update";
    public static final String PROJECT_DELETE = "project.delete";
    public static final String PROJECT_MANAGE_MEMBERS = "project.manage_members";

    private SystemRoles() {}
}
