import { getSession } from "@/lib/auth/session";
import { ApiError, gatewayBaseUrl, publicApiKey } from "@/lib/api/client";
import type { ApiResponse } from "@/lib/types/api";

export type ProjectDocument = {
  id: string;
  name: string;
  fileType: string;
  fileSize: number;
  version: number;
  projectId: string;
  entityType: string;
  entityId: string;
  uploadedBy: string;
  createdAt: string;
};

/** DOC-002 */
export function listProjectDocuments(projectId: string) {
  return fetchJson<{ items: ProjectDocument[]; total: number }>(
    `/api/v1/documents?projectId=${encodeURIComponent(projectId)}`
  );
}

/** DOC-001 multipart — not JSON, so it does not go through apiFetch. */
export async function uploadProjectDocument(projectId: string, file: File) {
  const session = getSession();
  if (!session?.accessToken || !session.organizationId) {
    throw new ApiError("Not authenticated", 401);
  }
  const body = new FormData();
  body.set("file", file);
  body.set("projectId", projectId);
  body.set("entityType", "PROJECT");
  body.set("entityId", projectId);
  body.set("name", file.name);
  const res = await fetch(`${gatewayBaseUrl()}/api/v1/documents/upload`, {
    method: "POST",
    headers: {
      "X-API-Key": publicApiKey(),
      Authorization: `Bearer ${session.accessToken}`,
      "X-Organization-Id": session.organizationId,
    },
    body,
  });
  return readEnvelope<ProjectDocument>(res);
}

/** DOC-003 — fetch with the same headers as other product calls, then save the file. */
export async function downloadProjectDocument(id: string, filename: string) {
  const session = getSession();
  if (!session?.accessToken || !session.organizationId) {
    throw new ApiError("Not authenticated", 401);
  }
  const res = await fetch(`${gatewayBaseUrl()}/api/v1/documents/${id}/content`, {
    headers: {
      "X-API-Key": publicApiKey(),
      Authorization: `Bearer ${session.accessToken}`,
      "X-Organization-Id": session.organizationId,
    },
  });
  if (!res.ok) {
    throw new ApiError(`Download failed (${res.status})`, res.status);
  }
  const blob = await res.blob();
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}

async function fetchJson<T>(path: string): Promise<T> {
  const session = getSession();
  if (!session?.accessToken || !session.organizationId) {
    throw new ApiError("Not authenticated", 401);
  }
  const res = await fetch(`${gatewayBaseUrl()}${path}`, {
    headers: {
      "X-API-Key": publicApiKey(),
      Accept: "application/json",
      Authorization: `Bearer ${session.accessToken}`,
      "X-Organization-Id": session.organizationId,
    },
  });
  return readEnvelope<T>(res);
}

async function readEnvelope<T>(res: Response): Promise<T> {
  const json = (await res.json().catch(() => null)) as ApiResponse<T> | null;
  if (!res.ok || (json && json.success === false)) {
    throw new ApiError(json?.message || `Request failed (${res.status})`, res.status);
  }
  return (json?.data ?? (json as unknown as T)) as T;
}
