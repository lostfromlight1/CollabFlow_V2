# CollabFlow — Future Plan (deferred / reduced services)

**Purpose:** Remember what we **cut or merged** when slimmed from ~16 services to the [MVP](services-mvp.md).  
**Slim MVP is complete** — Phase 2 starts with **admin-provisioned tenancy** (not documents first). Ordered waves: [phase-2.md](phase-2.md).

Canonical MVP services: [services-mvp.md](services-mvp.md).  
Endpoint / Kafka reference for the slim MVP: [microservice.md](microservice.md).

---

## What we kept (MVP) — for comparison

| Service | Lang | Notes |
|---------|------|--------|
| auth-service | Java | Kept as-is |
| platform-service | Java | **Merged** org + workspace + project |
| task-service | Java | **Merged** task + workflow (+ sprints later in same service) |
| api-gateway | Java | Edge routing |
| collab-service | Go | **Merged** notification + invitation + comment + activity (+ simple email) |
| realtime-service | Go | Kept as-is |

---

## Merged away (not separate services anymore)

These existed in the old plan as their own services. They are **folded into MVP** — only split again later if scale or team boundaries demand it.

| Old service name | Now lives in | When to split again? |
|------------------|--------------|----------------------|
| organization-service | `platform-service` | Rarely — only if org tenancy becomes huge |
| workspace-service | `platform-service` | Same |
| project-service | `platform-service` | Same |
| workflow-service | `task-service` | If workflow admin becomes a product of its own |
| notification-service | `collab-service` | If notification volume needs independent scaling |
| invitation-service | `collab-service` | Unlikely |
| comment-service | `collab-service` | If comments need dedicated search/moderation |
| activity-service | `collab-service` | If activity write load dwarfs comments/notifications |
| email-service | Stub / send inside `collab-service` for MVP | Extract when you need queues, templates, provider ops at scale |

---

## Deferred as future services (Phase 2+)

These were **reduced out of MVP** on purpose. Bring them back as new services (or modules) after auth → platform → task → collab → realtime is working.

### Phase 2 — Tenancy first, then media / email

**Ordered checklist (single source):** [phase-2.md](phase-2.md) — Waves 0→8. Tick boxes also live in each service README.

| Future service | Lang (suggested) | What it does | When |
|----------------|------------------|--------------|------|
| **user-service** | Java | Split from auth: profiles, admin user CRUD; auth keeps login/tokens/verify | Wave 3 (after admin APIs work on auth) |
| **document-service** | Go | Upload/download, folders, versions; MinIO/S3 | Wave 7 — **scaffold on :9083** (stubs + MinIO env); full storage later |
| **email-service** (standalone) | Go | SMTP/templates/retries | Wave 8 — **scaffold on :9084** (Kafka consume + SMTP-ready log); real SMTP later |

**Email in Phase 2A:** Auth (and later collab invites) **publish** Kafka `email.send`; **collab stub consumer** logs/sends. Wave 8 extract (`email-service`) can **coexist** with the stub; disable collab’s consumer later via `EMAIL_SERVICE_ENABLED=true` (documented, not wired).

Schema for documents already sketched in [database.md](database.md) (marked Phase 2).

**Product story:** Master **admin portal** creates orgs and users; gate public self-register and self-serve org create; login → email verify → org picker → `X-Organization-Id`. No personal/friends self-serve org.

| Area | README Phase 2 TODO |
|------|---------------------|
| Auth / admin seed / verify | [auth-service/README.md](../backend/java-services/auth-service/README.md) |
| Gateway / CORS / org header / sprints JWT | [api-gateway/README.md](../backend/java-services/api-gateway/README.md) |
| Platform admin org/member | [platform-service/README.md](../backend/java-services/platform-service/README.md) |
| Common DTOs / org header / request-id | [common/README.md](../backend/java-services/common/README.md) |
| Task sprints / filters | [task-service/README.md](../backend/java-services/task-service/README.md) |
| Collab invite + email stub | [collab-service/README.md](../backend/go-services/collab-service/README.md) |
| Realtime ACL | [realtime-service/README.md](../backend/go-services/realtime-service/README.md) |
| Documents (Wave 7) | [document-service/README.md](../backend/go-services/document-service/README.md) |
| Email extract (Wave 8) | [email-service/README.md](../backend/go-services/email-service/README.md) |

### Phase 3 — Insights & data out

| Future service | Lang (suggested) | What it does | Notes |
|----------------|------------------|--------------|--------|
| **analytics-service** | Go or Java | Workspace/project/user/sprint metrics | MVP uses simple count APIs on `task-service` / `platform-service` first |
| **export-service** | Go | CSV/Excel/PDF export jobs | Or start as an export module inside `task-service` |
| **search-service** | Go | Global/task/project search | Prefer Postgres full-text first; Elasticsearch later |

### Phase 4 — Enterprise / integrations

| Future capability | Notes |
|-------------------|--------|
| **webhook-service** (optional) | Outbound webhooks on Kafka events — was optional in features.md |
| API productization | Stronger rate limits, API keys, public OpenAPI — still via gateway |
| Database-per-service | Migrate off shared Postgres when ownership boundaries are stable |

---

## Suggested order to reintroduce

```
MVP done (6 services)
    │
    ▼
Phase 2 waves (see phase-2.md):
  W0 foundations → W1 admin/identity → W2 org session
  → W3 user-service → W4 collab/email stub → W5 task → W6 realtime
  → W7 document-service → W8 email-service extract
    │
    ▼
Phase 3: search (Postgres FTS → ES)  →  analytics-service  →  export-service
    │
    ▼
Phase 4: webhooks / API hardening / DB-per-service (optional)
```

---

## Learning note (Kaung)

Re-adding services is a **good second portfolio chapter**: “started with a slim cut, then extracted document-service when file storage and permissions needed a clear boundary.”  
Until then, resist splitting `platform` or `collab` just for symmetry with the old 16-service diagram.

---

## Checklist before extracting a new service

- [ ] Slim MVP is demoable end-to-end  
- [ ] Clear table ownership (update [database.md](database.md))  
- [ ] Clear Kafka topics (update [microservice.md](microservice.md) + [services-mvp.md](services-mvp.md))  
- [ ] New port + gateway route  
- [ ] Use skill `add-microservice` only after updating docs  

---

**Last updated:** 2026-10-23
