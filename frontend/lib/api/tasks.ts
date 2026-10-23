import { apiFetch, type PageResult } from "@/lib/api/client";

export type WorkflowStatus = {
  id: string;
  workflowId: string;
  name: string;
  displayOrder: number;
  color: string | null;
  category: string;
};

export type Workflow = {
  id: string;
  name: string;
  description: string | null;
  statuses: WorkflowStatus[];
};

export type TaskType = {
  id: string;
  name: string;
  description: string | null;
  defaultStatusId: string | null;
};

export type Task = {
  id: string;
  name: string;
  description: string | null;
  taskTypeId: string | null;
  projectId: string;
  workspaceId: string;
  organizationId: string;
  parentTaskId: string | null;
  statusId: string | null;
  assigneeId: string | null;
  reporterId: string | null;
  priority: string | null;
  position: number;
  startDate: string | null;
  dueDate: string | null;
  storyPoints: number | null;
  sprintId: string | null;
  createdAt: string;
  updatedAt: string;
};

export type TaskStats = {
  projectId: string;
  total: number;
  byStatus: { statusId: string; name: string; category: string; count: number }[];
  byPriority: { priority: string; count: number }[];
};

export type Sprint = {
  id: string;
  name: string;
  description: string | null;
  projectId: string;
  sprintNumber: number;
  startDate: string;
  endDate: string;
  status: string;
  goal: string | null;
  capacity: number | null;
  velocity: number | null;
};

/** WF-001 */
export function getDefaultWorkflow() {
  return apiFetch<Workflow>("/api/v1/workflows/default", { orgHeader: true });
}

/** WF-004 */
export function listTaskTypes() {
  return apiFetch<TaskType[]>("/api/v1/task-types", { orgHeader: true });
}

export type TaskQuery = {
  projectId: string;
  statusId?: string;
  assigneeId?: string;
  priority?: string;
  sprintId?: string;
  page?: number;
  size?: number;
};

/** TASK-002 */
export function listTasks(query: TaskQuery) {
  const params = new URLSearchParams({ projectId: query.projectId });
  if (query.statusId) params.set("statusId", query.statusId);
  if (query.assigneeId) params.set("assigneeId", query.assigneeId);
  if (query.priority) params.set("priority", query.priority);
  if (query.sprintId) params.set("sprintId", query.sprintId);
  params.set("page", String(query.page ?? 0));
  params.set("size", String(query.size ?? 20));
  return apiFetch<PageResult<Task>>(`/api/v1/tasks?${params}`, { orgHeader: true });
}

/** TASK-001 */
export function createTask(input: {
  projectId: string;
  name: string;
  description?: string;
  priority?: string;
  dueDate?: string;
}) {
  return apiFetch<Task>("/api/v1/tasks", {
    method: "POST",
    orgHeader: true,
    body: input,
  });
}

/** TASK-003 */
export function getTask(id: string) {
  return apiFetch<Task>(`/api/v1/tasks/${id}`, { orgHeader: true });
}

/** TASK-004 */
export function updateTask(
  id: string,
  input: { name?: string; description?: string; priority?: string; dueDate?: string | null }
) {
  return apiFetch<Task>(`/api/v1/tasks/${id}`, {
    method: "PUT",
    orgHeader: true,
    body: input,
  });
}

/** TASK-005 */
export function deleteTask(id: string) {
  return apiFetch<null>(`/api/v1/tasks/${id}`, { method: "DELETE", orgHeader: true });
}

/** TASK-006 */
export function changeTaskStatus(id: string, statusId: string) {
  return apiFetch<Task>(`/api/v1/tasks/${id}/status`, {
    method: "PUT",
    orgHeader: true,
    body: { statusId },
  });
}

/** TASK-007 */
export function assignTask(id: string, assigneeId: string | null) {
  return apiFetch<Task>(`/api/v1/tasks/${id}/assign`, {
    method: "PUT",
    orgHeader: true,
    body: { assigneeId },
  });
}

/** TASK-008 */
export function getTaskStats(projectId: string) {
  return apiFetch<TaskStats>(
    `/api/v1/tasks/stats?projectId=${encodeURIComponent(projectId)}`,
    { orgHeader: true }
  );
}

/** TASK-009 */
export function listSubtasks(id: string) {
  return apiFetch<Task[]>(`/api/v1/tasks/${id}/subtasks`, { orgHeader: true });
}

/** TASK-010 */
export function createSubtask(id: string, input: { name: string; description?: string; priority?: string }) {
  return apiFetch<Task>(`/api/v1/tasks/${id}/subtasks`, {
    method: "POST",
    orgHeader: true,
    body: input,
  });
}

/** SPRINT-002 */
export function listSprints(projectId: string) {
  return apiFetch<Sprint[]>(`/api/v1/sprints?projectId=${encodeURIComponent(projectId)}`, {
    orgHeader: true,
  });
}

/** SPRINT-001 */
export function createSprint(input: {
  projectId: string;
  name: string;
  description?: string;
  startDate: string;
  endDate: string;
  goal?: string;
}) {
  return apiFetch<Sprint>("/api/v1/sprints", { method: "POST", orgHeader: true, body: input });
}

/** SPRINT-003 */
export function getSprint(id: string) {
  return apiFetch<Sprint>(`/api/v1/sprints/${id}`, { orgHeader: true });
}

/** SPRINT-004 */
export function updateSprint(
  id: string,
  input: { name?: string; description?: string; goal?: string; startDate?: string; endDate?: string }
) {
  return apiFetch<Sprint>(`/api/v1/sprints/${id}`, { method: "PUT", orgHeader: true, body: input });
}

/** SPRINT-005 */
export function deleteSprint(id: string) {
  return apiFetch<null>(`/api/v1/sprints/${id}`, { method: "DELETE", orgHeader: true });
}

/** SPRINT-006 */
export function startSprint(id: string) {
  return apiFetch<Sprint>(`/api/v1/sprints/${id}/start`, { method: "POST", orgHeader: true });
}

/** SPRINT-007 */
export function completeSprint(id: string) {
  return apiFetch<Sprint>(`/api/v1/sprints/${id}/complete`, { method: "POST", orgHeader: true });
}

/** SPRINT-008 */
export function attachSprintTask(sprintId: string, taskId: string) {
  return apiFetch<Task>(`/api/v1/sprints/${sprintId}/tasks`, {
    method: "POST",
    orgHeader: true,
    body: { taskId },
  });
}

/** SPRINT-009 */
export function detachSprintTask(sprintId: string, taskId: string) {
  return apiFetch<Task>(`/api/v1/sprints/${sprintId}/tasks/${taskId}`, {
    method: "DELETE",
    orgHeader: true,
  });
}