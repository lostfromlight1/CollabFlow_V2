package com.collabflow.platform.security;

import java.util.UUID;

/** Stable IDs from V2 seed migration. */
public final class SystemRoles {

    public static final UUID ORGANIZATION_OWNER = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    public static final UUID ORGANIZATION_ADMIN = UUID.fromString("a0000000-0000-4000-8000-000000000002");
    public static final UUID ORGANIZATION_MEMBER = UUID.fromString("a0000000-0000-4000-8000-000000000003");

    public static final String ORG_READ = "organization.read";
    public static final String ORG_UPDATE = "organization.update";
    public static final String ORG_DELETE = "organization.delete";
    public static final String ORG_MANAGE_MEMBERS = "organization.manage_members";

    private SystemRoles() {}
}
