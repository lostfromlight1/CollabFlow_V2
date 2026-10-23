import { apiFetch } from "@/lib/api/client";

export type Workspace = {
  id: string;
  name: string;
  description: string | null;
  imageUrl: string | null;
  organizationId: string;
  ownerId: string;
  inviteCode: string | null;
  defaultWorkflowId: string | null;
  timeZone: string | null;
  dateFormat: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
};

export type Project = {
  id: string;
  name: string;
  description: string | null;
  imageUrl: string | null;
  workspaceId: string;
  parentProjectId: string | null;
  ownerId: string;
  status: string;
  projectType: string | null;
  startDate: string | null;
  dueDate: string | null;
  workflowId: string | null;
  leadId: string | null;
  createdAt: string;
  updatedAt: string;
};

/** WS-002 */
export function listWorkspaces(organizationId: string) {
  return apiFetch<Workspace[]>(
    `/api/v1/organizations/${organizationId}/workspaces`,
    { orgHeader: true }
  );
}

/** WS-001 */
export function createWorkspace(input: {
  organizationId: string;
  name: string;
  description?: string;
}) {
  return apiFetch<Workspace>("/api/v1/workspaces", {
    method: "POST",
    orgHeader: true,
    body: {
      organizationId: input.organizationId,
      name: input.name,
      description: input.description || undefined,
    },
  });
}

/** WS-003 */
export function getWorkspace(id: string) {
  return apiFetch<Workspace>(`/api/v1/workspaces/${id}`, { orgHeader: true });
}

/** PRJ-002 */
export function listProjects(workspaceId: string) {
  return apiFetch<Project[]>(`/api/v1/workspaces/${workspaceId}/projects`, {
    orgHeader: true,
  });
}

/** PRJ-001 */
export function createProject(input: {
  workspaceId: string;
  name: string;
  description?: string;
  projectType?: string;
}) {
  return apiFetch<Project>("/api/v1/projects", {
    method: "POST",
    orgHeader: true,
    body: {
      workspaceId: input.workspaceId,
      name: input.name,
      description: input.description || undefined,
      projectType: input.projectType || "CUSTOM",
    },
  });
}

/** PRJ-003 */
export function getProject(id: string) {
  return apiFetch<Project>(`/api/v1/projects/${id}`, { orgHeader: true });
}

export type Organization = {
  id: string;
  name: string;
  description: string | null;
  logoUrl: string | null;
  ownerId: string;
  slug: string;
  plan: string | null;
  maxMembers: number | null;
  maxWorkspaces: number | null;
  active: boolean;
  roleId: string | null;
  roleName: string | null;
};

export type Member = {
  id: string;
  userId: string;
  roleId: string;
  roleName: string;
  joinedAt: string;
  name: string | null;
  email: string | null;
  organizationId?: string;
  workspaceId?: string;
  projectId?: string;
};

export type RoleOption = {
  id: string;
  name: string;
  description: string | null;
  scope: string;
};

/** ORG-003 */
export function getOrganization(id: string) {
  return apiFetch<Organization>(`/api/v1/organizations/${id}`, { orgHeader: true });
}

/** ORG-001 */
export function createOrganization(input: { name: string; slug: string; description?: string }) {
  return apiFetch<Organization>("/api/v1/organizations", {
    method: "POST",
    orgHeader: true,
    body: input,
  });
}

/** ORG-004 */
export function updateOrganization(
  id: string,
  input: { name?: string; description?: string; logoUrl?: string }
) {
  return apiFetch<Organization>(`/api/v1/organizations/${id}`, {
    method: "PUT",
    orgHeader: true,
    body: input,
  });
}

/** ORG-005 */
export function deleteOrganization(id: string) {
  return apiFetch<null>(`/api/v1/organizations/${id}`, {
    method: "DELETE",
    orgHeader: true,
  });
}

/** ORG-006 */
export function listOrgMembers(id: string) {
  return apiFetch<Member[]>(`/api/v1/organizations/${id}/members`, { orgHeader: true });
}

/** ORG-007 */
export function addOrgMember(id: string, input: { userId: string; roleId: string }) {
  return apiFetch<Member>(`/api/v1/organizations/${id}/members`, {
    method: "POST",
    orgHeader: true,
    body: input,
  });
}

/** ORG-008 */
export function updateOrgMemberRole(orgId: string, memberId: string, roleId: string) {
  return apiFetch<Member>(`/api/v1/organizations/${orgId}/members/${memberId}`, {
    method: "PUT",
    orgHeader: true,
    body: { roleId },
  });
}

/** ORG-009 */
export function removeOrgMember(orgId: string, memberId: string) {
  return apiFetch<null>(`/api/v1/organizations/${orgId}/members/${memberId}`, {
    method: "DELETE",
    orgHeader: true,
  });
}

/** ORG-010 */
export function transferOrganization(id: string, newOwnerUserId: string) {
  return apiFetch<Organization>(`/api/v1/organizations/${id}/transfer-ownership`, {
    method: "POST",
    orgHeader: true,
    body: { newOwnerUserId },
  });
}

/** ROLE-001 */
export function listRoles(scope: string) {
  return apiFetch<RoleOption[]>(`/api/v1/roles?scope=${encodeURIComponent(scope)}`, {
    orgHeader: true,
  });
}

/** WS-004 */
export function updateWorkspace(
  id: string,
  input: { name?: string; description?: string; timeZone?: string; dateFormat?: string }
) {
  return apiFetch<Workspace>(`/api/v1/workspaces/${id}`, {
    method: "PUT",
    orgHeader: true,
    body: input,
  });
}

/** WS-005 */
export function deleteWorkspace(id: string) {
  return apiFetch<null>(`/api/v1/workspaces/${id}`, { method: "DELETE", orgHeader: true });
}

/** WS-006 */
export function listWorkspaceMembers(id: string) {
  return apiFetch<Member[]>(`/api/v1/workspaces/${id}/members`, { orgHeader: true });
}

/** WS-007 */
export function addWorkspaceMember(id: string, input: { userId: string; roleId: string }) {
  return apiFetch<Member>(`/api/v1/workspaces/${id}/members`, {
    method: "POST",
    orgHeader: true,
    body: input,
  });
}

/** WS-008 */
export function updateWorkspaceMemberRole(workspaceId: string, memberId: string, roleId: string) {
  return apiFetch<Member>(`/api/v1/workspaces/${workspaceId}/members/${memberId}`, {
    method: "PUT",
    orgHeader: true,
    body: { roleId },
  });
}

/** WS-009 */
export function removeWorkspaceMember(workspaceId: string, memberId: string) {
  return apiFetch<null>(`/api/v1/workspaces/${workspaceId}/members/${memberId}`, {
    method: "DELETE",
    orgHeader: true,
  });
}

/** WS-010 */
export function joinWorkspace(id: string, inviteCode: string) {
  return apiFetch<Member>(`/api/v1/workspaces/${id}/join`, {
    method: "POST",
    orgHeader: true,
    body: { inviteCode },
  });
}

/** PRJ-004 */
export function updateProject(
  id: string,
  input: {
    name?: string;
    description?: string;
    projectType?: string;
    status?: string;
  }
) {
  return apiFetch<Project>(`/api/v1/projects/${id}`, {
    method: "PUT",
    orgHeader: true,
    body: input,
  });
}

/** PRJ-005 archive */
export function archiveProject(id: string) {
  return apiFetch<null>(`/api/v1/projects/${id}`, { method: "DELETE", orgHeader: true });
}

/** PRJ-006 */
export function listProjectMembers(id: string) {
  return apiFetch<Member[]>(`/api/v1/projects/${id}/members`, { orgHeader: true });
}

/** PRJ-007 */
export function addProjectMember(id: string, input: { userId: string; roleId: string }) {
  return apiFetch<Member>(`/api/v1/projects/${id}/members`, {
    method: "POST",
    orgHeader: true,
    body: input,
  });
}

/** PRJ-008 */
export function updateProjectMemberRole(projectId: string, memberId: string, roleId: string) {
  return apiFetch<Member>(`/api/v1/projects/${projectId}/members/${memberId}`, {
    method: "PUT",
    orgHeader: true,
    body: { roleId },
  });
}

/** PRJ-009 */
export function removeProjectMember(projectId: string, memberId: string) {
  return apiFetch<null>(`/api/v1/projects/${projectId}/members/${memberId}`, {
    method: "DELETE",
    orgHeader: true,
  });
}
