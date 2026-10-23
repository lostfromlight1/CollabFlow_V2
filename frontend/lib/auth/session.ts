import type { SessionUser } from "@/lib/types/api";

const KEY = "collabflow.session";
const BACKUP_KEY = "collabflow.adminBackup";

export type Session = {
  accessToken: string;
  refreshToken: string;
  user: SessionUser;
  organizationId: string | null;
  organizations: { id: string; name: string; slug: string }[];
  impersonating?: boolean;
};

function canUseStorage(): boolean {
  return typeof window !== "undefined";
}

export function getSession(): Session | null {
  if (!canUseStorage()) return null;
  const raw = sessionStorage.getItem(KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as Session;
  } catch {
    return null;
  }
}

export function setSession(session: Session): void {
  if (!canUseStorage()) return;
  sessionStorage.setItem(KEY, JSON.stringify(session));
}

export function clearSession(): void {
  if (!canUseStorage()) return;
  sessionStorage.removeItem(KEY);
  sessionStorage.removeItem(BACKUP_KEY);
}

export function updateOrganizationId(organizationId: string | null): void {
  const s = getSession();
  if (!s) return;
  setSession({ ...s, organizationId });
}

/** Keep the signed-in user when AUTH-004 rotates tokens. */
export function updateTokens(input: {
  accessToken: string;
  refreshToken: string;
  organizationId?: string | null;
}): Session | null {
  const s = getSession();
  if (!s) return null;
  const next: Session = {
    ...s,
    accessToken: input.accessToken,
    refreshToken: input.refreshToken,
    organizationId:
      input.organizationId === undefined ? s.organizationId : input.organizationId,
  };
  setSession(next);
  return next;
}

/** Swap in a read-only impersonation token and keep the admin session for Exit. */
export function beginImpersonation(input: { accessToken: string; user: SessionUser }): void {
  const current = getSession();
  if (!current || !canUseStorage()) return;
  sessionStorage.setItem(BACKUP_KEY, JSON.stringify({ ...current, impersonating: false }));
  setSession({
    ...current,
    accessToken: input.accessToken,
    refreshToken: "",
    user: input.user,
    organizationId: null,
    organizations: [],
    impersonating: true,
  });
}

export function endImpersonationRestore(): Session | null {
  if (!canUseStorage()) return null;
  const raw = sessionStorage.getItem(BACKUP_KEY);
  sessionStorage.removeItem(BACKUP_KEY);
  if (!raw) return null;
  try {
    const backup = JSON.parse(raw) as Session;
    const next = { ...backup, impersonating: false };
    setSession(next);
    return next;
  } catch {
    return null;
  }
}
