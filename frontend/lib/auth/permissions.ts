/** Seeded role ids from platform Flyway V1–V4. */
export const ROLES = {
  ORG_OWNER: "a0000000-0000-4000-8000-000000000001",
  ORG_ADMIN: "a0000000-0000-4000-8000-000000000002",
  ORG_MEMBER: "a0000000-0000-4000-8000-000000000003",
  WS_ADMIN: "a0000000-0000-4000-8000-000000000004",
  WS_MEMBER: "a0000000-0000-4000-8000-000000000005",
  WS_VIEWER: "a0000000-0000-4000-8000-000000000006",
  PRJ_ADMIN: "a0000000-0000-4000-8000-000000000007",
  PRJ_MEMBER: "a0000000-0000-4000-8000-000000000008",
  PRJ_VIEWER: "a0000000-0000-4000-8000-000000000009",
} as const;

export function isOrgOwner(roleId?: string | null) {
  return roleId === ROLES.ORG_OWNER;
}

export function canManageOrg(roleId?: string | null) {
  return roleId === ROLES.ORG_OWNER || roleId === ROLES.ORG_ADMIN;
}

export function canManageWorkspace(roleId?: string | null) {
  return roleId === ROLES.WS_ADMIN;
}

export function canCreateProject(roleId?: string | null) {
  return roleId === ROLES.WS_ADMIN;
}

export function canManageProject(roleId?: string | null) {
  return roleId === ROLES.PRJ_ADMIN;
}

export function canWriteTasks(roleId?: string | null) {
  return roleId === ROLES.PRJ_ADMIN || roleId === ROLES.PRJ_MEMBER;
}

export function isProjectViewer(roleId?: string | null) {
  return roleId === ROLES.PRJ_VIEWER;
}