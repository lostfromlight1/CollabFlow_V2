# task-service

**Port:** 8083 · Java 21 / Boot 4.1 / Gradle  
**Owns:** status_workflows, workflow_statuses, workflow_transitions, task_types, tasks  
(+ sprints / custom fields later)

## Week 8 — Workflows + tasks

- **Seeded default workflow** (Kanban): Backlog → To Do → In Progress → In Review → Done, forward transitions only
- System task types: Task, Bug, Story, Epic
- Task CRUD under a project; list by `projectId`
- Status changes enforce `workflow_transitions`; `closed_at` set on DONE
- Assign / unassign (`assignee` must be project member)
- AuthZ via shared DB: `project_members` + `task.*` permissions (Admin all; Member no delete; Viewer read)
- Kafka after commit: `task.created`, `task.updated`, `task.deleted`, `task.assigned`, `task.status_changed`

Stable seed IDs:

| Entity | UUID |
|--------|------|
| Default workflow | `c0000000-0000-4000-8000-000000000001` |
| Status To Do | `c0000000-0000-4000-8000-000000000012` |
| Status In Progress | `c0000000-0000-4000-8000-000000000013` |
| Status Done | `c0000000-0000-4000-8000-000000000015` |
| Type Task | `d0000000-0000-4000-8000-000000000001` |

## Run

Needs Postgres (auth + platform migrations first, including projects), Kafka. Prefer gateway `:8080`.  
Task migrations use `flyway_task_history` (V1 workflows + tasks).

```powershell
cd D:\Codes\CollabFlow_V2\java-services
.\gradlew.bat :task-service:bootRun
```

Swagger: http://localhost:8083/swagger-ui.html

## Headers

- `X-API-Key`
- `Authorization: Bearer <accessToken>`

## Conventions

- Controllers: `@CurrentUserId` + `@ApiId` — see [services-mvp.md](../../docs/services-mvp.md)
- Logs: `logs/task/task-service.yyyy-MM-dd.log` under repo root when using `bootRun`
