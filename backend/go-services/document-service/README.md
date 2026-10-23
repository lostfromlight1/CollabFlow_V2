# document-service

**Port:** 9083 · Go **1.27** / Gin + GORM  
**Owns:** `documents` (AutoMigrate, same pattern as collab-service)  
**Storage:** local directory `DOCUMENT_STORAGE_DIR` (default `data/documents`). Slim Compose has no MinIO, so bytes stay on disk and the row stores the file id as `file_path`.

Java analogy: a `@RestController` plus a `JdbcTemplate` membership check. JWT is HS256 with the user id in `sub`, same as the Java resource server.

## Endpoints

| Id | Method | Path | Notes |
|----|--------|------|-------|
| DOC-000 | GET | `/api/v1/documents/health` | Public health |
| DOC-001 | POST | `/api/v1/documents/upload` | Multipart `file` + `projectId`. Caller must be a project member |
| DOC-002 | GET | `/api/v1/documents?projectId=` | Latest files for that project |
| DOC-003 | GET | `/api/v1/documents/{id}/content` | Download. Same membership check |

## Config

| Variable | Default |
|----------|---------|
| `DOCUMENT_HTTP_PORT` | `9083` |
| `DOCUMENT_STORAGE_DIR` | `data/documents` |
| `POSTGRES_*` | same as other services |
| `JWT_SECRET` | same secret as auth-service |

## Verify

```bash
cd backend/go-services/document-service
go test ./...
```

Uploading the same name again creates a new version and marks the previous row `is_latest = false`.
