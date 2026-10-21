# auth-service

**Port:** 8081 · Java 21 / Boot 4.1 / Gradle  
**Owns:** `users`, `refresh_tokens`

## Layout

`controller` / `service` / `dto` / `repository` / `model` / `config` / `exception` / `event`  
Shared: `:common` → envelopes, `ApiKeyFilter`, `DomainEvent`.

## Week 4 extras (learn these)

| Piece | What / why |
|-------|------------|
| **Kafka `user.created`** | After DB commit only — announce new users for collab later (notifications). Never publish if the transaction rolled back. |
| **Redis rate limit** | `INCR` + TTL on login/register by client IP — shared across instances. Fails open if Redis is down. |
| **Gateway JWT** | Prefer calling via `:8080`; gateway verifies Bearer before proxying `/me`. |

## Security

| Layer | How |
|-------|-----|
| Client gate | Header `X-API-Key` = env `API_KEY` |
| User auth | `Authorization: Bearer` (JWT from `JWT_SECRET`) |

Swagger: http://localhost:8081/swagger-ui.html

## Run

Needs Docker: Postgres, Redis, Kafka (`infrastructure/`).

```powershell
cd D:\Codes\CollabFlow_V2\java-services
.\gradlew.bat :auth-service:bootRun
```

## Try it (direct or via gateway `:8080`)

```powershell
$k = "6e7f9c2b1a3d5e7f8b4fdf31e2f3a4b5c"
$base = "http://localhost:8080"   # or http://localhost:8081

curl "$base/api/v1/auth/health" -H "X-API-Key: $k"

curl -X POST "$base/api/v1/auth/register" -H "Content-Type: application/json" -H "X-API-Key: $k" -d "{\"email\":\"kaung@example.com\",\"password\":\"password123\",\"name\":\"Kaung\"}"

curl -X POST "$base/api/v1/auth/login" -H "Content-Type: application/json" -H "X-API-Key: $k" -d "{\"email\":\"kaung@example.com\",\"password\":\"password123\"}"

curl "$base/api/v1/auth/me" -H "X-API-Key: $k" -H "Authorization: Bearer <accessToken>"
```

Watch Kafka (optional):

```powershell
docker exec -it collabflow-kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic user.created --from-beginning
```

Postman: `postman/CollabFlow-Auth.postman_collection.json` (set `baseUrl` to `http://localhost:8080` to go through the gateway).

**Note:** Login is email + password only for now; organization comes later.
