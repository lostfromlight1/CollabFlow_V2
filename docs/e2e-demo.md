# CollabFlow — E2E demo script (Week 13)

Run infra + all 6 services, then walk this path once for a portfolio recording.

## Prereqs

```powershell
cd D:\Codes\CollabFlow_V2\infrastructure
docker compose --env-file ../.env up -d
# Expect: postgres, redis, zookeeper, kafka healthy
```

Start (separate terminals): **gateway :8080**, **auth :8081**, **platform :8082**, **task :8083**, **collab :9081**, **realtime :9082**.

Same `JWT_SECRET` / `API_KEY` / Postgres as `.env`.

## Happy path

| Step | Action | Expect |
|------|--------|--------|
| 1 | Postman **Auth → Register** | `accessToken`, `userId` |
| 2 | Auth → **Register other user** | `otherUserId`, `otherAccessToken` |
| 3 | Platform → create **org → workspace → project** | ids in env |
| 4 | Add other user as org/workspace/project member (or invite accept) | membership OK |
| 5 | Tasks → create task → **assign** to self or other | Kafka `task.assigned` |
| 6 | Collab up **before** assign | `GET /notifications` has `TASK_ASSIGNED`; log `EMAIL_STUB` |
| 7 | `GET /activity?entityType=TASK&entityId=` | `TASK_ASSIGNED` row; actor = assigner |
| 8 | WS: `ws://localhost:9082/api/v1/realtime/ws?token=` → subscribe `project:{{projectId}}` | connected |
| 9 | Update task name **or** re-assign / change status | WS `task.updated` |
| 10 | Comment with `@{{otherUserId}}` | comment + mention notif; WS `comment.added` if subscribed to `task:` |
| 11 | `GET /api/v1/tasks/stats?projectId=` | totals by status + priority |

## Cross-service checks

- **Viewer** role: can `GET` tasks/stats; cannot assign/update (403).
- Assign before collab starts → no notification (`LastOffset`); re-assign after collab is up.
- Gateway needs `X-API-Key` + Bearer on HTTP; WS may use `?token=` (+ `?apiKey=` via gateway).

## Done when

You can narrate: *register → tenancy → task → Kafka → notification + activity → live WS*, without restarting mid-demo except the known “start collab first” rule.
