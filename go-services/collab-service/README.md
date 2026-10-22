# collab-service

**Port:** 9081 · Go **1.27** / Gin / GORM / kafka-go  
**Owns:** notifications (Week 9); invitations, comments, activity later (Weeks 10–11)

## Week 9 — done

| Phase | What |
|-------|------|
| 1 | Module + Gin health |
| 2 | Config + GORM AutoMigrate (`notifications`, `processed_events`) |
| 3 | Kafka consumer `task.assigned` → notification |
| 4 | REST list / mark read + JWT |
| 5 | Gateway routes + Postman folder 8 |

## Run

Postgres + Kafka up. Same DB as Java (`collabflow`).

```powershell
cd D:\Codes\CollabFlow_V2\go-services\collab-service
go run ./cmd/server
```

- Health: http://localhost:9081/api/v1/collab/health  
- Via gateway: http://localhost:8080/api/v1/collab/health (restart gateway after Week 9 route change)

## Smoke

1. Start collab (`go run ./cmd/server`)  
2. Assign a task (Postman Tasks or folder 8)  
3. Watch log: `notification created ...`  
4. `GET /api/v1/notifications` with Bearer  

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
internal/kafka/        # consumer ≈ @KafkaListener
```
