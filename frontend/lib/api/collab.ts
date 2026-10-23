import { apiFetch, type PageResult } from "@/lib/api/client";

export type Invitation = {
  id: string;
  email: string;
  organizationId: string;
  workspaceId: string | null;
  projectId: string | null;
  invitedBy: string;
  invitationType: string;
  status: string;
  token: string;
  expiresAt: string;
  createdAt: string;
};

export type Comment = {
  id: string;
  content: string;
  entityType: string;
  entityId: string;
  authorId: string;
  parentCommentId: string | null;
  createdAt: string;
  updatedAt: string;
};

export type ActivityItem = {
  id: string;
  actionType: string;
  entityType: string;
  entityId: string;
  organizationId: string;
  projectId: string | null;
  userId: string;
  actorName: string;
  actorEmail: string;
  createdAt: string;
};

export type NotificationItem = {
  id: string;
  title: string;
  message: string;
  notificationType: string;
  entityType: string;
  entityId: string;
  isRead: boolean;
  actionUrl: string | null;
  createdAt: string;
};

/** INV-001 */
export function createInvitation(input: {
  email: string;
  organizationId: string;
  invitationType?: string;
}) {
  return apiFetch<Invitation>("/api/v1/invitations", {
    method: "POST",
    orgHeader: true,
    body: { invitationType: "ORGANIZATION", ...input },
  });
}

/** INV-002 public peek */
export function peekInvitation(token: string) {
  return apiFetch<Invitation>(`/api/v1/invitations/token/${encodeURIComponent(token)}`, {
    auth: false,
  });
}

/** INV-003 */
export function listMyInvitations(page = 0, size = 20) {
  return apiFetch<PageResult<Invitation>>(`/api/v1/invitations?page=${page}&size=${size}`, {
    orgHeader: true,
  });
}

/** INV-004 */
export function listOrgInvitations(organizationId: string, page = 0, size = 20) {
  return apiFetch<PageResult<Invitation>>(
    `/api/v1/invitations/org?organizationId=${encodeURIComponent(organizationId)}&page=${page}&size=${size}`,
    { orgHeader: true }
  );
}

/** INV-005 */
export function acceptInvitation(token: string) {
  return apiFetch<Invitation>(`/api/v1/invitations/token/${encodeURIComponent(token)}/accept`, {
    method: "POST",
  });
}

/** INV-006 decline / cancel */
export function cancelInvitation(id: string) {
  return apiFetch<null>(`/api/v1/invitations/${id}`, { method: "DELETE", orgHeader: true });
}

/** CMT-001 */
export function createComment(input: { content: string; entityType: string; entityId: string }) {
  return apiFetch<Comment>("/api/v1/comments", { method: "POST", orgHeader: true, body: input });
}

/** CMT-002 */
export function listComments(entityType: string, entityId: string) {
  const q = new URLSearchParams({ entityType, entityId });
  return apiFetch<Comment[]>(`/api/v1/comments?${q}`, { orgHeader: true });
}

/** CMT-004 */
export function updateComment(id: string, content: string) {
  return apiFetch<Comment>(`/api/v1/comments/${id}`, {
    method: "PUT",
    orgHeader: true,
    body: { content },
  });
}

/** CMT-005 */
export function deleteComment(id: string) {
  return apiFetch<null>(`/api/v1/comments/${id}`, { method: "DELETE", orgHeader: true });
}

/** ACT-001 */
export function listOrgActivity(page = 0, size = 20) {
  return apiFetch<PageResult<ActivityItem>>(`/api/v1/activity?page=${page}&size=${size}`, {
    orgHeader: true,
  });
}

/** ACT-003 */
export function listEntityActivity(type: string, id: string, page = 0, size = 20) {
  return apiFetch<PageResult<ActivityItem>>(
    `/api/v1/activity/entity/${encodeURIComponent(type)}/${id}?page=${page}&size=${size}`,
    { orgHeader: true }
  );
}

/** ACT-004 — product activity for one user. No org header; master admin can call it. */
export function listUserActivity(userId: string, page = 0, size = 20) {
  return apiFetch<PageResult<ActivityItem>>(
    `/api/v1/activity/user/${userId}?page=${page}&size=${size}`
  );
}

/** NOTIF-001 */
export function listNotifications(page = 0, size = 20) {
  return apiFetch<PageResult<NotificationItem>>(
    `/api/v1/notifications?page=${page}&size=${size}`,
    { orgHeader: true }
  );
}

/** NOTIF-002 */
export function markNotificationRead(id: string) {
  return apiFetch<null>(`/api/v1/notifications/${id}/read`, { method: "PUT", orgHeader: true });
}

/** NOTIF-003 */
export function markAllNotificationsRead() {
  return apiFetch<null>("/api/v1/notifications/read-all", { method: "PUT", orgHeader: true });
}

/** NOTIF-004 */
export function unreadNotificationCount() {
  return apiFetch<{ count: number }>("/api/v1/notifications/unread-count", { orgHeader: true });
}