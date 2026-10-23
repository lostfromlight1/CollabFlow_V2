export type ApiResponse<T> = {
  success: boolean;
  message: string;
  data: T;
  timestamp?: string;
};

export type OrganizationSummary = {
  id: string;
  name: string;
  slug: string;
  roleId: string;
};

export type AuthResponse = {
  userId: string;
  email: string;
  name: string;
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  selectedOrganizationId: string | null;
  organizations: OrganizationSummary[];
  requiresOrganizationSelection: boolean;
};

export type UserProfile = {
  id: string;
  email: string;
  name: string;
  avatarUrl: string | null;
  emailVerified: boolean;
  active: boolean;
  masterAdmin: boolean;
};

export type SessionUser = {
  id: string;
  email: string;
  name: string;
  masterAdmin: boolean;
};
