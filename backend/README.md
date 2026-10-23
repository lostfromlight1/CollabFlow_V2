# CollabFlow backend

Java + Go microservices and local Docker infra.

| Path | Contents |
|------|----------|
| [java-services/](java-services/) | auth, platform, task, user, api-gateway, common |
| [go-services/](go-services/) | collab, realtime, email, document |
| [infrastructure/](infrastructure/) | Postgres, Redis, Kafka, MailHog |

**How to run:** [docs/running.md](../docs/running.md)  
**FE contract:** [docs/fe-contract.md](../docs/fe-contract.md)  
**Secrets:** repo-root `.env` (not `application.yml`). Gradle `bootRun` loads it automatically.

## Quick ports

| Port | Service |
|------|---------|
| 8080 | api-gateway |
| 8081 | auth-service |
| 8082 | platform-service |
| 8083 | task-service |
| 8084 | user-service |
| 9081 | collab-service |
| 9082 | realtime-service |
| 9083 | document-service (stubs) |
| 9084 | email-service |

```powershell
# Infra
cd backend\infrastructure
docker compose --env-file ..\..\.env up -d

# Java (from backend/java-services)
.\gradlew.bat :auth-service:bootRun

# Go
cd backend\go-services\collab-service
go run ./cmd/server
```
