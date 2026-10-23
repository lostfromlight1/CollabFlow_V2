import { apiFetch, gatewayBaseUrl, publicApiKey, type PageResult } from "@/lib/api/client";
import { getSession } from "@/lib/auth/session";
import type { UserProfile } from "@/lib/types/api";

/** System org role: Organization Member (platform Flyway V1). */
export const ORG_MEMBER_ROLE_ID = "a0000000-0000-4000-8000-000000000003";

export type AdminUser = {
  id: string;
  email: string;
  name: string;
  avatarUrl: string | null;
  emailVerified: boolean;
  active: boolean;
  masterAdmin: boolean;
  createdAt: string;
  lastLoginAt: string | null;
  deletedAt: string | null;
};

export type AdminOrganization = {
  id: string;
  name: string;
  description?: string | null;
  slug: string;
  plan: string;
  active: boolean;
  createdAt: string;
  ownerId: string;
  ownerName: string | null;
  ownerEmail: string | null;
};

export type AdminWorkspace = {
  id: string;
  name: string;
  active: boolean;
  projectCount: number;
};

export type AdminOrganizationDetail = {
  organization: AdminOrganization;
  memberCount: number;
  workspaceCount: number;
  projectCount: number;
  workspaces: AdminWorkspace[];
};

export type AdminMembership = {
  organizationId: string;
  name: string;
  slug: string;
  active: boolean;
  roleId: string;
  roleName: string;
};

export type AdminMember = {
  id: string;
  userId: string;
  organizationId: string;
  roleId: string;
  roleName: string;
  name: string | null;
  email: string | null;
};

export type SignupDay = { date: string; count: number };

export type AuditItem = {
  id: string;
  actorUserId: string;
  action: string;
  targetType: string;
  targetId: string | null;
  metadata: string | null;
  ipAddress: string | null;
  createdAt: string;
};

export type AdminStats = {
  usersTotal: number;
  usersActive: number;
  usersInactive: number;
  usersDeleted: number;
  masterAdmins: number;
  verifiedUsers: number;
  signupsLast7Days: number;
  signupsLast30Days: number;
  activeUsersLast7Days: number;
  orgsTotal: number;
  orgsActive: number;
  orgsInactive: number;
  signupsByDay: SignupDay[];
  recentAudit: AuditItem[];
};

export type Announcement = {
  id: string;
  title: string;
  body: string;
  active: boolean;
  startsAt: string | null;
  endsAt: string | null;
  createdBy: string;
  createdAt: string;
};

export type FeatureFlag = {
  key: string;
  enabled: boolean;
  description: string | null;
  updatedAt: string;
};

export type ImpersonationResult = {
  accessToken: string;
  expiresAt: string;
  targetUserId: string;
  targetEmail: string;
  targetName: string;
};

function qs(params: Record<string, string | number | boolean | undefined | null>): string {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value === undefined || value === null || value === "") continue;
    search.set(key, String(value));
  }
  const text = search.toString();
  return text ? `?${text}` : "";
}

export function adminCreateUser(input: {
  email: string;
  temporaryPassword: string;
  name: string;
}) {
  return apiFetch<UserProfile>("/api/v1/admin/users", { method: "POST", body: input });
}

export function listAdminUsers(query: Record<string, string | number | boolean | undefined | null>) {
  return apiFetch<PageResult<AdminUser>>(`/api/v1/admin/users${qs(query)}`);
}

export function getAdminUser(id: string) {
  return apiFetch<AdminUser>(`/api/v1/admin/users/${id}`);
}

export function setMasterAdmin(id: string, masterAdmin: boolean) {
  return apiFetch<null>(`/api/v1/admin/users/${id}/master-admin`, {
    method: "PUT",
    body: { masterAdmin },
  });
}

export function deleteAdminUser(id: string) {
  return apiFetch<null>(`/api/v1/admin/users/${id}/delete`, { method: "POST" });
}

export function restoreAdminUser(id: string) {
  return apiFetch<null>(`/api/v1/admin/users/${id}/restore`, { method: "POST" });
}

export function deactivateAdminUser(id: string) {
  return apiFetch<null>(`/api/v1/admin/users/${id}/deactivate`, { method: "POST" });
}

export function activateAdminUser(id: string) {
  return apiFetch<null>(`/api/v1/admin/users/${id}/activate`, { method: "POST" });
}

export function revokeAdminUserSessions(id: string) {
  return apiFetch<null>(`/api/v1/admin/users/${id}/sessions/revoke`, { method: "POST" });
}

export function resetAdminUserPassword(id: string, temporaryPassword: string) {
  return apiFetch<null>(`/api/v1/admin/users/${id}/reset-password`, {
    method: "POST",
    body: { temporaryPassword },
  });
}

export function resendAdminVerification(id: string) {
  return apiFetch<null>(`/api/v1/admin/users/${id}/resend-verification`, { method: "POST" });
}

export function adminCreateOrganization(input: {
  name: string;
  slug: string;
  description?: string;
  ownerUserId: string;
}) {
  return apiFetch<AdminOrganization>("/api/v1/admin/organizations", { method: "POST", body: input });
}

export function listAdminOrganizations(query: Record<string, string | number | boolean | undefined | null>) {
  return apiFetch<PageResult<AdminOrganization>>(`/api/v1/admin/organizations${qs(query)}`);
}

export function getAdminOrganization(id: string) {
  return apiFetch<AdminOrganizationDetail>(`/api/v1/admin/organizations/${id}`);
}

export function listAdminOrgMembers(id: string, page = 0, size = 20) {
  return apiFetch<PageResult<AdminMember>>(
    `/api/v1/admin/organizations/${id}/members?page=${page}&size=${size}`
  );
}

export function listMembershipsForUser(userId: string) {
  return apiFetch<AdminMembership[]>(`/api/v1/admin/organizations/by-user/${userId}`);
}

export function deactivateAdminOrganization(id: string) {
  return apiFetch<null>(`/api/v1/admin/organizations/${id}/deactivate`, { method: "POST" });
}

export function activateAdminOrganization(id: string) {
  return apiFetch<null>(`/api/v1/admin/organizations/${id}/activate`, { method: "POST" });
}

export function adminTransferOwnership(organizationId: string, newOwnerUserId: string) {
  return apiFetch<AdminOrganization>(
    `/api/v1/admin/organizations/${organizationId}/transfer-ownership`,
    { method: "POST", body: { newOwnerUserId } }
  );
}

export function adminAttachMember(organizationId: string, input: { userId: string; roleId?: string }) {
  return apiFetch<AdminMember>(`/api/v1/admin/organizations/${organizationId}/members`, {
    method: "POST",
    body: { userId: input.userId, roleId: input.roleId || ORG_MEMBER_ROLE_ID },
  });
}

export function getAdminStats() {
  return apiFetch<AdminStats>("/api/v1/admin/stats");
}

export function listAdminAudit(query: Record<string, string | number | boolean | undefined | null>) {
  return apiFetch<PageResult<AuditItem>>(`/api/v1/admin/audit${qs(query)}`);
}

export function listAnnouncements() {
  return apiFetch<Announcement[]>("/api/v1/admin/announcements");
}

export function createAnnouncement(input: {
  title: string;
  body: string;
  active: boolean;
  startsAt?: string | null;
  endsAt?: string | null;
}) {
  return apiFetch<Announcement>("/api/v1/admin/announcements", { method: "POST", body: input });
}

export function updateAnnouncement(id: string, input: {
  title: string;
  body: string;
  active: boolean;
  startsAt?: string | null;
  endsAt?: string | null;
}) {
  return apiFetch<Announcement>(`/api/v1/admin/announcements/${id}`, { method: "PUT", body: input });
}

export function deleteAnnouncement(id: string) {
  return apiFetch<null>(`/api/v1/admin/announcements/${id}`, { method: "DELETE" });
}

export function getActiveAnnouncement() {
  return apiFetch<Announcement | null>("/api/v1/announcements/active");
}

export function listAdminFlags() {
  return apiFetch<FeatureFlag[]>("/api/v1/admin/feature-flags");
}

export function listFeatureFlags() {
  return apiFetch<FeatureFlag[]>("/api/v1/feature-flags");
}

export function saveFeatureFlag(key: string, input: { enabled: boolean; description?: string }) {
  return apiFetch<FeatureFlag>(`/api/v1/admin/feature-flags/${encodeURIComponent(key)}`, {
    method: "PUT",
    body: input,
  });
}

export function deleteFeatureFlag(key: string) {
  return apiFetch<null>(`/api/v1/admin/feature-flags/${encodeURIComponent(key)}`, { method: "DELETE" });
}

export function startImpersonation(userId: string) {
  return apiFetch<ImpersonationResult>("/api/v1/admin/sessions/impersonate", {
    method: "POST",
    body: { userId },
  });
}

export function endImpersonation() {
  return apiFetch<null>("/api/v1/admin/sessions/impersonate/end", {
    method: "POST",
    skipRefresh: true,
  });
}

export async function downloadAdminCsv(path: string, filename: string): Promise<void> {
  const session = getSession();
  if (!session?.accessToken) {
    throw new Error("Not authenticated");
  }
  const res = await fetch(`${gatewayBaseUrl()}${path}`, {
    headers: {
      "X-API-Key": publicApiKey(),
      Authorization: `Bearer ${session.accessToken}`,
      Accept: "text/csv",
    },
  });
  if (!res.ok) {
    let message = `Export failed (${res.status})`;
    try {
      const json = (await res.json()) as { message?: string };
      if (json.message) message = json.message;
    } catch {
      // CSV error bodies are sometimes plain text
    }
    throw new Error(message);
  }
  const blob = await res.blob();
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}
