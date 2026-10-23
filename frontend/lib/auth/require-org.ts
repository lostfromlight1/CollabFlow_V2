import { getSession } from "@/lib/auth/session";

/** Returns session + organizationId, or null if missing (caller should redirect). */
export function requireOrgSession() {
  const s = getSession();
  if (!s?.accessToken) return null;
  if (!s.organizationId) return null;
  return s;
}

export function orgDisplayName(organizationId: string | null): string {
  if (!organizationId) return "No organization";
  const s = getSession();
  const hit = s?.organizations?.find((o) => o.id === organizationId);
  return hit?.name ?? organizationId.slice(0, 8) + "…";
}
