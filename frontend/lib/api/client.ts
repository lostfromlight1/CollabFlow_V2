import { endImpersonationRestore, getSession, updateTokens } from "@/lib/auth/session";
import type { ApiResponse, AuthResponse } from "@/lib/types/api";

export class ApiError extends Error {
  status: number;

  constructor(message: string, status: number) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

function baseUrl(): string {
  const url = process.env.NEXT_PUBLIC_API_BASE_URL;
  if (!url) {
    throw new Error("NEXT_PUBLIC_API_BASE_URL is not set");
  }
  return url.replace(/\/$/, "");
}

function apiKey(): string {
  const key = process.env.NEXT_PUBLIC_API_KEY;
  if (!key) {
    throw new Error("NEXT_PUBLIC_API_KEY is not set");
  }
  return key;
}

export function gatewayBaseUrl(): string {
  return baseUrl();
}

export function publicApiKey(): string {
  return apiKey();
}

type RequestOptions = {
  method?: string;
  body?: unknown;
  auth?: boolean;
  orgHeader?: boolean;
  /** Skip the one-shot AUTH-004 retry (used by refresh itself). */
  skipRefresh?: boolean;
};

let refreshInflight: Promise<string | null> | null = null;

async function refreshAccessToken(): Promise<string | null> {
  if (refreshInflight) return refreshInflight;
  refreshInflight = (async () => {
    const session = getSession();
    if (!session?.refreshToken) return null;
    try {
      const res = await fetch(`${baseUrl()}/api/v1/auth/refresh`, {
        method: "POST",
        headers: {
          "X-API-Key": apiKey(),
          Accept: "application/json",
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ refreshToken: session.refreshToken }),
      });
      const json = (await res.json().catch(() => null)) as ApiResponse<AuthResponse> | null;
      const data = json?.data;
      if (!res.ok || !json?.success || !data?.accessToken || !data.refreshToken) {
        return null;
      }
      updateTokens({
        accessToken: data.accessToken,
        refreshToken: data.refreshToken,
        organizationId: data.selectedOrganizationId ?? session.organizationId,
      });
      return data.accessToken;
    } catch {
      return null;
    } finally {
      refreshInflight = null;
    }
  })();
  return refreshInflight;
}

export async function apiFetch<T>(
  path: string,
  options: RequestOptions = {}
): Promise<T> {
  return request<T>(path, options, false);
}

async function request<T>(
  path: string,
  options: RequestOptions,
  retried: boolean
): Promise<T> {
  const { method = "GET", body, auth = true, orgHeader = false } = options;
  const headers: Record<string, string> = {
    "X-API-Key": apiKey(),
    Accept: "application/json",
  };
  if (body !== undefined) {
    headers["Content-Type"] = "application/json";
  }

  const session = getSession();
  const impersonating = Boolean(session?.impersonating);
  if (auth) {
    if (!session?.accessToken) {
      throw new ApiError("Not authenticated", 401);
    }
    headers.Authorization = `Bearer ${session.accessToken}`;
  }
  if (orgHeader) {
    if (!session?.organizationId) {
      throw new ApiError("X-Organization-Id required — select an organization", 400);
    }
    headers["X-Organization-Id"] = session.organizationId;
  }

  const res = await fetch(`${baseUrl()}${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });

  if (res.status === 401 && impersonating) {
    endImpersonationRestore();
    if (typeof window !== "undefined") {
      window.location.assign("/admin");
    }
    throw new ApiError("Impersonation ended", 401);
  }

  if (
    res.status === 401 &&
    auth &&
    !retried &&
    !options.skipRefresh &&
    !impersonating &&
    !path.startsWith("/api/v1/auth/refresh") &&
    !path.startsWith("/api/v1/auth/login")
  ) {
    const next = await refreshAccessToken();
    if (next) {
      return request<T>(path, options, true);
    }
  }

  let json: ApiResponse<T> | null = null;
  try {
    json = (await res.json()) as ApiResponse<T>;
  } catch {
    // non-JSON error body
  }

  if (!res.ok) {
    const msg =
      json?.message ||
      (json as unknown as { error?: string })?.error ||
      `Request failed (${res.status})`;
    throw new ApiError(msg, res.status);
  }

  if (json && typeof json.success === "boolean" && !json.success) {
    throw new ApiError(json.message || "Request failed", res.status);
  }

  return (json?.data ?? (json as unknown as T)) as T;
}

export type PageResult<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};
