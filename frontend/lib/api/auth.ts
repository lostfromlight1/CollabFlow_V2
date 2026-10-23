import { apiFetch } from "@/lib/api/client";
import type { AuthResponse, OrganizationSummary, UserProfile } from "@/lib/types/api";

export function login(email: string, password: string) {
  return apiFetch<AuthResponse>("/api/v1/auth/login", {
    method: "POST",
    auth: false,
    body: { email, password },
  });
}

export function getMyProfile() {
  return apiFetch<UserProfile>("/api/v1/users/me");
}

export function listMyOrganizations() {
  return apiFetch<OrganizationSummary[]>("/api/v1/auth/organizations");
}

export function selectOrganization(organizationId: string) {
  return apiFetch<AuthResponse>("/api/v1/auth/select-organization", {
    method: "POST",
    body: { organizationId },
  });
}

export function logout(refreshToken: string) {
  return apiFetch<null>("/api/v1/auth/logout", {
    method: "POST",
    auth: false,
    body: { refreshToken },
  }).catch(() => null);
}

/** AUTH-002 — server may reject when public register is off. */
export function register(input: { email: string; password: string; name: string }) {
  return apiFetch<AuthResponse>("/api/v1/auth/register", {
    method: "POST",
    auth: false,
    body: input,
  });
}

/** AUTH-009 */
export function resendVerification(email: string) {
  return apiFetch<null>("/api/v1/auth/resend-verification", {
    method: "POST",
    auth: false,
    body: { email },
  });
}

/** AUTH-010 */
export function forgotPassword(email: string) {
  return apiFetch<null>("/api/v1/auth/forgot-password", {
    method: "POST",
    auth: false,
    body: { email },
  });
}

/** AUTH-011 */
export function resetPassword(token: string, newPassword: string) {
  return apiFetch<null>("/api/v1/auth/reset-password", {
    method: "POST",
    auth: false,
    body: { token, newPassword },
  });
}

/** USER-002 */
export function updateMyProfile(input: { name?: string; avatarUrl?: string }) {
  return apiFetch<UserProfile>("/api/v1/users/me", {
    method: "PUT",
    body: input,
  });
}

/** AUTH-012 */
export function changePassword(currentPassword: string, newPassword: string) {
  return apiFetch<null>("/api/v1/auth/change-password", {
    method: "POST",
    body: { currentPassword, newPassword },
  });
}

export type AuthSessionRow = {
  id: string;
  createdAt: string;
  expiresAt: string;
};

/** AUTH-013 */
export function listSessions() {
  return apiFetch<AuthSessionRow[]>("/api/v1/auth/sessions");
}

/** AUTH-014 */
export function revokeAllSessions() {
  return apiFetch<null>("/api/v1/auth/sessions/revoke-all", { method: "POST" });
}

/** AUTH-015 */
export function deactivateAccount() {
  return apiFetch<null>("/api/v1/auth/deactivate", { method: "POST" });
}
