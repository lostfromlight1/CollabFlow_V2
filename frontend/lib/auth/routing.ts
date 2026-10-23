import type { AuthResponse, SessionUser } from "@/lib/types/api";
import { setSession, type Session } from "@/lib/auth/session";

export function sessionFromAuth(
  auth: AuthResponse,
  user: SessionUser
): Session {
  return {
    accessToken: auth.accessToken,
    refreshToken: auth.refreshToken,
    user,
    organizationId: auth.selectedOrganizationId,
    organizations: (auth.organizations || []).map((o) => ({
      id: o.id,
      name: o.name,
      slug: o.slug,
    })),
  };
}

export function persistAuth(auth: AuthResponse, user: SessionUser): Session {
  const session = sessionFromAuth(auth, user);
  setSession(session);
  return session;
}

/** Where to send the user after login / select-org. */
export function postAuthPath(
  user: SessionUser,
  auth: Pick<AuthResponse, "requiresOrganizationSelection">
): string {
  if (auth.requiresOrganizationSelection) return "/select-org";
  if (user.masterAdmin) return "/admin";
  return "/home";
}
