# CollabFlow — Future Plan (deferred / reduced services)

**Purpose:** Remember what we **cut or merged** when slimmed from ~16 services to the [MVP](services-mvp.md).  
**Do not build these as separate deployables until the slim MVP works end-to-end.**

Canonical MVP services: [services-mvp.md](services-mvp.md).  
Old full catalog (endpoints/events): [microservice.md](microservice.md).

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

### Phase 2 — Collaboration & media

| Future service | Lang (suggested) | What it does | Depends on |
|----------------|------------------|--------------|------------|
| **document-service** | Go | Upload/download, folders, versions, permissions; MinIO/S3 | platform (entity IDs), auth |
| **email-service** (standalone) | Go | Queue, templates, SendGrid/Mailgun, retries | Kafka `email.send`, Redis queue |

Schema for documents already sketched in [database.md](database.md) (marked Phase 2).

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
Phase 2: document-service  →  standalone email-service (if needed)
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

**Last updated:** 2026-10-21
