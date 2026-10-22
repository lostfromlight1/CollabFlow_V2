# collab-service

**Port:** 9081 · Go **1.27** / Gin / GORM / kafka-go  
**Owns:** notifications, invitations, comments, activity_feed (+ email log stub)

## What / why

Collab handles async side effects after Java writes: Kafka events become notifications and activity rows. Mentions and invites are sync HTTP on the same service so the MVP stays one Go deployable.

## Weeks

| Week | Scope |
|------|--------|
| 9 | `task.assigned` → notification + REST |
| 10 | Invitations, comments, `@UUID` mentions, `comment.added` produce |
| 11 | Activity feed from Kafka + email stub (log only) |

## Run

Postgres + Kafka up. Same DB as Java (`collabflow`).

```powershell
cd D:\Codes\CollabFlow_V2\go-services\collab-service
go run ./cmd/server
```

## Smoke (Week 11)

1. Start collab **before** assign  
2. Assign a task → log: `notification created` + `activity TASK_ASSIGNED` + `EMAIL_STUB`  
3. `GET /api/v1/activity?entityType=TASK&entityId={{taskId}}`  
4. Create a comment → `activity COMMENT_ADDED`

## Env

| Variable | Default |
|----------|---------|
| `COLLAB_HTTP_PORT` | `9081` |
| `POSTGRES_*` | same as Java `.env` |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` |
| `KAFKA_GROUP_ID` | `collab-service` |
| `JWT_SECRET` | same as auth/gateway |

## Layout

```
cmd/server/main.go
internal/config/
internal/database/
internal/model/
internal/repository/
internal/service/
internal/handler/
internal/middleware/   # JWT ≈ Spring Security filter
internal/kafka/        # consumers ≈ @KafkaListener
internal/email/        # StubSend log only
```
