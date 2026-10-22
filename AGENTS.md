# CollabFlow — Agent Guide

How to work on this repo in Cursor. Prefer this file before inventing APIs, tables, or events.

## Product

Multi-tenant collaboration / project management.  
**Hierarchy:** Organization → Workspace → Project → Task (sub-tasks supported).

## Stack (slim MVP)

Pinned versions: **[docs/tech-stack.md](docs/tech-stack.md)** (wins on conflicts).  
Services: **[docs/services-mvp.md](docs/services-mvp.md)** — **6 deployables**.

| Layer | Choice |
|-------|--------|
| Java | **21 LTS** + **Spring Boot 4.1.x** + **Gradle** — auth, platform, task, api-gateway |
| Spring Cloud | **2025.1.x** (Gateway) |
| Golang | **1.27** + Gin/GORM/kafka-go — collab, realtime |
| Database | **PostgreSQL 17** — shared DB for MVP |
| Cache | **Redis 8.x** |
| Events | **Kafka 3.9 + ZooKeeper** (Confluent Platform **7.9.x**) |
| Frontend (later) | Next.js (version pinned when started) |

## Build order

1. **[docs/implementation-guide.md](docs/implementation-guide.md)** week-by-week  
2. Check off **[docs/progress-tracker.md](docs/progress-tracker.md)**  
3. Order: infra → gateway → **auth** → **platform** → **task** → **collab** → **realtime** → analytics/polish  

Do **not** follow [docs/archive/legacy-16-service-plan.md](docs/archive/legacy-16-service-plan.md).

## Doc map

| Need | Doc |
|------|-----|
| PC setup (Windows/Fedora) | [docs/setup.md](docs/setup.md) |
| Implementation + weeks | [docs/implementation-guide.md](docs/implementation-guide.md) |
| Progress tracker | [docs/progress-tracker.md](docs/progress-tracker.md) |
| Services / ports / ApiId catalog | [docs/services-mvp.md](docs/services-mvp.md) |
| Tech versions | [docs/tech-stack.md](docs/tech-stack.md) |
| Future / deferred | [docs/future-plan.md](docs/future-plan.md) |
| Features catalog | [docs/features.md](docs/features.md) |
| Endpoints / events ref | [docs/microservice.md](docs/microservice.md) |
| Schema | [docs/database.md](docs/database.md) |
| Legacy 16-service plan | [docs/archive/legacy-16-service-plan.md](docs/archive/legacy-16-service-plan.md) (**archive only**) |

## Working rules

- Boundaries: `docs/services-mvp.md` wins over the 16-service layout  
- Versions: `docs/tech-stack.md` wins  
- Do not invent tables/endpoints/topics; update docs in the same change  
- Shared Postgres for MVP; Kafka after successful DB write  
- Java first; Go with skill `teach-and-build`  
- Learning/portfolio mode: always-on rule `.cursor/rules/collabflow-core.mdc`  
- Java controllers: `@CurrentUserId` + `@ApiId` — see `.cursor/rules/java-spring.mdc` and ApiId catalog in `docs/services-mvp.md`

## Project skills

| Skill | When |
|-------|------|
| `teach-and-build` | Go, Kafka, Redis, microservice learning |
| `plan-mvp-feature` | Map a feature before coding |
| `add-microservice` | Scaffold slim-list service only |
| `add-kafka-event` | New domain event |
| `add-flyway-migration` | Schema change |

## Cursor how-to

- Rules: `collabflow-core` (always), plus `java-spring` / `golang-services` / `database-postgres` by glob  
- Name a skill in chat when you want that workflow  
- Plan mode for multi-service design; Agent mode for focused edits  
- Point at `docs/implementation-guide.md` or `docs/progress-tracker.md` when starting a week

## Repo status

Slim MVP docs + Java core in progress (auth, gateway, platform org).  
Folders: `java-services/`, `go-services/`, `infrastructure/`, `docs/`.
