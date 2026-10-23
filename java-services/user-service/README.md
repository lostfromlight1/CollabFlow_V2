# user-service

**Port:** 8084 · Java 21 / Boot 4.1 / Gradle  
**Phase 2 Wave 3:** Split from auth — profiles + admin user CRUD  
**Ordered waves:** [docs/phase-2.md](../../docs/phase-2.md)

## Ownership

| Owns | Notes |
|------|--------|
| Profile APIs | `GET/PUT /api/v1/users/me`, deactivate |
| Admin create user | `POST /api/v1/admin/users` |
| Kafka | `user.created`, `user.updated` |

**Shared table `users`:** schema Flyway remains on **auth-service**. Auth keeps login/tokens/verify/password/email_tokens.

### Auth vs user (FE / UAT)

| Need | Call | Service |
|------|------|---------|
| Login, refresh, verify, password, org picker | `/api/v1/auth/**` | **auth** `:8081` |
| Profile `GET/PUT /users/me` | `/api/v1/users/**` | **user** `:8084` |
| Admin create user | `POST /api/v1/admin/users` | **user** `:8084` |

Both must be running for Phase 2 admin tenancy. Gateway 500 on `/admin/users` usually means user-service is down. Datasource uses the same `POSTGRES_*` as auth (repo-root `.env`).

```powershell
.\gradlew.bat :user-service:bootRun
```

## See also

| Topic | Owner |
|-------|--------|
| Login / JWT / verify | [auth-service](../auth-service/README.md) |
| Gateway `/users/**` | [api-gateway](../api-gateway/README.md) |
| Attach to org | [platform-service](../platform-service/README.md) |
