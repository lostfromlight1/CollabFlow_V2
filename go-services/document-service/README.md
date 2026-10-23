# document-service

**Port:** 9083 · Go **1.27** / Gin  
**Phase:** Wave 7 scaffold (Phase 2)  
**UAT:** **Out of scope** — stubs only; do not demo upload/list ([fe-contract.md](../../docs/fe-contract.md)).  
**Owns (planned):** `documents`, `document_folders`, `document_permissions`, `document_tags` — [database.md](../../docs/database.md)  
**Storage (planned):** MinIO/S3 via `MINIO_*` env  

Catalog: [services-mvp.md](../../docs/services-mvp.md) · Waves: [phase-2.md](../../docs/phase-2.md) · Deferred story: [future-plan.md](../../docs/future-plan.md)

### See also / Depends on

| Topic | Owner |
|-------|--------|
| Org/project membership for ACL | [platform-service](../../java-services/platform-service/README.md) |
| Gateway `/api/v1/documents/**` | [api-gateway](../../java-services/api-gateway/README.md) → `:9083` |
| Schema sketch | [database.md](../../docs/database.md) Document Management Tables |

## What / why

File attachments need a clear boundary (upload, versions, folders) separate from task/comment APIs. This Wave 7 scaffold is a **runnable Gin shell**: health + upload/list stubs + MinIO config placeholders. No MinIO client and no Flyway yet — ship the deployable and route first, then wire storage.

Java analogy: a Spring Boot app with `@RestController` stubs and `@Value` for S3 endpoint/bucket before the SDK is added.

## Endpoints (scaffold)

| Id | Method | Path | Notes |
|----|--------|------|-------|
| DOC-000 | GET | `/api/v1/documents/health` | Public health |
| DOC-001 | POST | `/api/v1/documents/upload` | Stub — accepts form fields; does not store |
| DOC-002 | GET | `/api/v1/documents?projectId=` | Stub — empty list |

Gateway: `/api/v1/documents/**` → `http://localhost:9083`.

## Config (env)

| Variable | Default | Purpose |
|----------|---------|---------|
| `DOCUMENT_HTTP_PORT` | `9083` | HTTP listen port |
| `MINIO_ENDPOINT` | `localhost:9000` | Object store host (placeholder) |
| `MINIO_BUCKET` | `collabflow-documents` | Bucket name (placeholder) |
| `MINIO_ACCESS_KEY` | `minioadmin` | Access key (placeholder) |
| `MINIO_SECRET_KEY` | `minioadmin` | Secret key (placeholder) |
| `MINIO_USE_SSL` | `false` | TLS flag (placeholder) |

## Run / verify

```bash
cd go-services/document-service
go run ./cmd/server
curl http://localhost:9083/api/v1/documents/health
curl "http://localhost:9083/api/v1/documents?projectId=00000000-0000-0000-0000-000000000001"
```

## Phase 2 — TODO (after scaffold)

- [ ] Postgres models + AutoMigrate / Flyway ownership decision for document tables
- [ ] Real MinIO upload/download + version rows (`is_latest`, `parent_document_id`)
- [ ] JWT + `X-Organization-Id` + project membership checks
- [ ] Folders / permissions / tags APIs
- [ ] Kafka `document.uploaded` / `document.deleted` when needed

## Learning note

**Why a separate service?** Object storage, large payloads, and document ACL grow differently from task CRUD. Keeping a thin HTTP surface now makes the later MinIO cutover a portfolio-friendly extract, not a rewrite of collab.
