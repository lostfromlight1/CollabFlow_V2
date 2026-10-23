# CollabFlow — Run the project & services

How to start **infra + apps** after tools are installed.  
Install JDK / Go / Docker: **[setup.md](setup.md)**.  
Smoke / Postman path: **[e2e-demo.md](e2e-demo.md)**.

---

## Doc map

| Doc | Use when |
|-----|----------|
| [setup.md](setup.md) | First-time machine: Git, JDK 21, Go 1.27, Docker |
| **This file** | `.env`, Compose, start every service, health checks |
| [e2e-demo.md](e2e-demo.md) | Admin tenancy demo + curl / Postman narrative |
| [infrastructure/README.md](../backend/infrastructure/README.md) | Compose flags, wipe volumes, Kafka smoke |
| [services-mvp.md](services-mvp.md) | Ports, gateway routes, ApiIds |

---

## 1. One-time: `.env`

From **repo root**:

```powershell
Copy-Item .env.example .env
# Linux/macOS: cp .env.example .env
```

Edit only if you need different secrets. Required for apps:

| Variable | Used by |
|----------|---------|
| `JWT_SECRET` | All JWT issuers/validators (Java + Go) |
| `API_KEY` | Gateway + services that call each other |
| `POSTGRES_*` | Compose + JDBC/GORM |
| `SMTP_*` / `EMAIL_SERVICE_ENABLED` | MailHog + email-service + collab stub switch |
| `MASTER_ADMIN_*` | Auth seed on first boot |

**Java:** `gradlew …:bootRun` loads repo-root `.env` automatically.  
**Go:** does **not** — export vars in the shell (see §4) or set them in the IDE.

Never put secrets in `application.yml`.

---

## 2. Infrastructure (Docker)

```powershell
cd backend\infrastructure
docker compose --env-file ..\..\.env up -d
docker compose --env-file ..\..\.env ps
```

Expect healthy / running:

| Container | Host ports | Role |
|-----------|------------|------|
| `collabflow-postgres` | 5432 | Shared DB |
| `collabflow-redis` | 6379 | Rate-limit + realtime pub/sub |
| `collabflow-zookeeper` | 2181 | Kafka metadata |
| `collabflow-kafka` | 9092 | Domain events |
| `collabflow-mailhog` | **1025** SMTP, **8025** UI | Catch outbound mail |

MailHog UI: http://localhost:8025

Smoke:

```powershell
docker exec -it collabflow-postgres pg_isready -U postgres
docker exec -it collabflow-redis redis-cli ping
```

Details / wipe volumes: [infrastructure/README.md](../backend/infrastructure/README.md).

---

## 3. Port map (apps)

### MVP (6)

| Port | Service | Folder |
|------|---------|--------|
| 8080 | api-gateway | `backend/java-services/api-gateway` |
| 8081 | auth-service | `backend/java-services/auth-service` |
| 8082 | platform-service | `backend/java-services/platform-service` |
| 8083 | task-service | `backend/java-services/task-service` |
| 9081 | collab-service | `backend/go-services/collab-service` |
| 9082 | realtime-service | `backend/go-services/realtime-service` |

### Phase 2 (add when demoing admin / email)

| Port | Service | Folder |
|------|---------|--------|
| 8084 | user-service | `backend/java-services/user-service` |
| 9084 | email-service | `backend/go-services/email-service` |
| 9083 | document-service | `backend/go-services/document-service` |

Clients should hit the **gateway** (`:8080`) with `X-API-Key` (+ Bearer where needed).  
Swagger (direct ports): auth `8081`, platform `8082`, task `8083` → `/swagger-ui.html`.

---

## 4. Start everything (one command)

From the repo root:

```powershell
scripts\dev-backend.cmd start
scripts\dev-backend.cmd status
scripts\dev-backend.cmd stop
```

`start` brings up Docker infra, then every Java and Go app in the background. Logs and process ids go to `logs/backend/` (gitignored). `stop` kills those app process trees and leaves Postgres/Redis/Kafka running so data stays. The first start is slow because Gradle and `go run` compile.

Each app is still its own process and port. The script only starts them for you. The manual commands below are what it runs.

Suggested order so Flyway / Kafka consumers do not race emptily:

1. **Infra** (Compose) — wait until Kafka is up  
2. **auth** → **platform** → **user** (Phase 2) → **task**  
3. **api-gateway** (or anytime after auth exists for health)  
4. **collab** → **realtime** → **email** (Phase 2)

You can open one terminal per service (or IDE run configs).

### Java (from `backend/java-services/`)

```powershell
cd backend\java-services

.\gradlew.bat :auth-service:bootRun
.\gradlew.bat :platform-service:bootRun
.\gradlew.bat :user-service:bootRun
.\gradlew.bat :task-service:bootRun
.\gradlew.bat :api-gateway:bootRun
```

Linux/macOS: `./gradlew :auth-service:bootRun` (same module names).

### Go — load `.env` then run

**PowerShell** (repo root, then each service in its own window):

```powershell
# Once per terminal: load KEY=VALUE from repo-root .env
Get-Content .env | Where-Object { $_ -match '^\s*[^#][^=]+=' } | ForEach-Object {
  $k, $v = $_ -split '=', 2
  Set-Item -Path "Env:$($k.Trim())" -Value $v.Trim()
}

cd backend\go-services\collab-service;   go run ./cmd/server
# new window + reload .env:
cd backend\go-services\realtime-service; go run ./cmd/server
cd backend\go-services\email-service;    go run ./cmd/server
```

**Bash:**

```bash
set -a && source .env && set +a
cd backend/go-services/collab-service && go run ./cmd/server
```

With `EMAIL_SERVICE_ENABLED=true`, collab **skips** its email stub — run **email-service** or mail never leaves Kafka.

---

## 5. Profiles (what to start)

| Goal | Start |
|------|--------|
| Auth only / login | infra + auth + gateway |
| Hierarchy + tasks | + platform + task |
| Phase 2 admin tenancy + SMTP | + **user** + **email** (+ MailHog) |
| Notifications / invites / comments | + **collab** |
| Live WS rooms | + **realtime** (needs Redis) |
| Documents (stubs) | + document-service (optional) |

Full portfolio demo ≈ all MVP + user + email.

---

## 6. Quick health checks

```powershell
$k = (Get-Content .env | Where-Object { $_ -match '^API_KEY=' }) -replace '^API_KEY=',''

curl -s http://localhost:8080/api/v1/auth/health -H "X-API-Key: $k"
curl -s http://localhost:9081/api/v1/collab/health
curl -s http://localhost:9082/api/v1/realtime/health
curl -s http://localhost:9084/api/v1/email/health   # if email-service up
```

Then: [e2e-demo.md](e2e-demo.md) or Postman folder **Auth → 4. Phase 2 admin tenancy**.

---

## 7. Stop / clean

```powershell
# Apps started by the script:
scripts\dev-backend.cmd stop

# Apps started by hand: Ctrl+C in each terminal

cd infrastructure
docker compose --env-file ../../.env stop          # keep data
docker compose --env-file ../../.env down          # remove containers, keep volumes
docker compose --env-file ../../.env down -v       # wipe DB/Redis volumes (fresh Flyway)
```

---

## 8. Run troubleshooting

| Problem | Fix |
|---------|-----|
| `Missing API_KEY / JWT_SECRET` on bootRun | Create repo-root `.env` from `.env.example` |
| Go missing `API_KEY` / wrong JWT | Load `.env` into that shell (§4); `JWT_SECRET` must match Java |
| Kafka connection refused | Wait for `collabflow-kafka`; check `docker compose ps` / logs |
| Port already in use | Stop the other process or change the service port env |
| No mail in MailHog | email-service running? `EMAIL_SERVICE_ENABLED=true`? Kafka up? Check email-service logs |
| Admin login fails | `MASTER_ADMIN_*` in `.env`; restart auth after changing seed vars |
| Gateway 401/403 | Send `X-API-Key` matching `.env`; Bearer for protected routes |
| Postgres auth failed after changing password | Volume still has old password → `down -v` then `up -d` |

Install / Docker Desktop / Go blocked by Windows policy → [setup.md](setup.md) troubleshooting.

---

**Last updated:** 2026-10-23
