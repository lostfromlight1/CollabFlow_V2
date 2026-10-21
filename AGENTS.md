# CollabFlow — Agent Guide

How to work on this repo in Cursor. Prefer this file and `docs/index.md` before inventing APIs, tables, or events.

## Product

CollabFlow is a multi-tenant collaboration / project-management platform.

**Hierarchy:** Organization → Workspace → Project → Task (sub-tasks supported).

## Stack (slim MVP)

Pinned versions: **[docs/tech-stack.md](docs/tech-stack.md)** (wins on version conflicts).

| Layer | Choice |
|-------|--------|
| Services | [docs/services-mvp.md](docs/services-mvp.md) — **6 deployables** |
| Java | **21 LTS** + **Spring Boot 4.1.x** + **Gradle** — auth, platform, task, api-gateway |
| Spring Cloud | **2025.1.x** (Gateway) |
| Golang | **1.27** + Gin/GORM/kafka-go — collab, realtime |
| Database | **PostgreSQL 17** — shared DB for MVP |
| Cache | **Redis 8.x** |
| Events | **Kafka 3.9 + ZooKeeper** (Confluent Platform **7.9.x**) |
| Frontend (later) | Next.js (version pinned when started) |

## Build order (follow the guide)

1. Use **[docs/implementation-guide.md](docs/implementation-guide.md)** week-by-week  
2. Check off **[docs/progress-tracker.md](docs/progress-tracker.md)**  
3. Order: infra → gateway → **auth** → **platform** → **task** → **collab** → **realtime** → analytics/polish  

Do **not** follow [project_overview/implementation.md](project_overview/implementation.md) (legacy 16-service plan).

## Doc map

| Need | Doc |
|------|-----|
| Index | [docs/index.md](docs/index.md) |
| PC setup (Windows/Fedora) | [docs/setup.md](docs/setup.md) |
| Implementation + weeks | [docs/implementation-guide.md](docs/implementation-guide.md) |
| Progress tracker | [docs/progress-tracker.md](docs/progress-tracker.md) |
| Services / ports | [docs/services-mvp.md](docs/services-mvp.md) |
| Tech versions | [docs/tech-stack.md](docs/tech-stack.md) |
| Future / deferred | [docs/future-plan.md](docs/future-plan.md) |
| Features catalog | [project_overview/feature.md](project_overview/feature.md) |
| Endpoints / events ref | [docs/microservice.md](docs/microservice.md) |
| Schema | [docs/database.md](docs/database.md) |

## Working rules

- Boundaries: `docs/services-mvp.md` wins over the 16-service layout  
- Versions: `docs/tech-stack.md` wins  
- Do not invent tables/endpoints/topics; update docs in the same change  
- Shared Postgres for MVP; Kafka after successful DB write  
- Java first; Go with skill `teach-and-build`  
- Learning/portfolio mode: `.cursor/rules/kaung-learning-portfolio.mdc`

## Project skills

| Skill | When |
|-------|------|
| `teach-and-build` | Go, Kafka, Redis, microservice learning |
| `plan-mvp-feature` | Map a feature before coding |
| `add-microservice` | Scaffold slim-list service only |
| `add-kafka-event` | New domain event |
| `add-flyway-migration` | Schema change |

## Cursor how-to

- Rules auto-apply (`collabflow-core`, `kaung-learning-portfolio`)  
- Name a skill in chat when you want that workflow  
- Plan mode for multi-service design; Agent mode for focused edits  
- Point at `docs/implementation-guide.md` or `docs/progress-tracker.md` when starting a week

## Repo status

Documentation finalized for slim MVP. Application code not started.  
Folders when coding: `java-services/`, `go-services/`, `infrastructure/`, `docs/`.
