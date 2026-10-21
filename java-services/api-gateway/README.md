# api-gateway

**Port:** 8080 · Java 21 / Spring Cloud Gateway / Gradle  
**Role:** Route `/api/v1/**`, require `X-API-Key`, validate JWT on protected routes.

## Security at the edge

| Check | Where |
|-------|--------|
| `X-API-Key` | All `/api/**` (same `API_KEY` as services) |
| Bearer JWT | `GET /api/v1/auth/me` (same `JWT_SECRET` as auth-service) |
| Public | register, login, refresh, logout, health |

## Run

```powershell
cd D:\Codes\CollabFlow_V2\java-services
# Terminal A: auth-service (needs Postgres + Redis + Kafka from docker compose)
.\gradlew.bat :auth-service:bootRun
# Terminal B:
.\gradlew.bat :api-gateway:bootRun
```

```powershell
$k = "6e7f9c2b1a3d5e7f8b4fdf31e2f3a4b5c"
curl http://localhost:8080/api/v1/auth/health -H "X-API-Key: $k"
```

Week 4 flow: register/login via `:8080` → copy `accessToken` → `GET /api/v1/auth/me` with Bearer + API key.

Routes: [docs/services-mvp.md](../../docs/services-mvp.md).
