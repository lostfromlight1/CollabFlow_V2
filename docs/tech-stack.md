# CollabFlow — Technology Stack (pinned)

**Policy:** Prefer **current OSS-supported** stable releases (no milestones/RCs). Pin majors for reproducibility; bump patch/minor deliberately.

**Status:** Slim MVP **complete** (Weeks 1–14).  
**Last reviewed:** 2026-10-22  

Canonical services: [services-mvp.md](services-mvp.md). Progress: [progress-tracker.md](progress-tracker.md).

---

## Decision summary

| Area | Choice | Why |
|------|--------|-----|
| Java | **21 (LTS)** | Long support; Boot 4 fully supports it |
| Spring Boot | **4.1.1** (allow 4.1.x patches) | Boot 3.5 OSS EOL; 4.1.x current stable |
| Spring Cloud | **2025.1.3** Oakwood | Official train for Boot 4.1 (`api-gateway`) |
| Build | **Gradle 8.14.3** Wrapper | Boot 4.1 requires Gradle 8.14+ |
| Go | **1.27** (`go 1.27` in modules) | Current stable Go line |
| DB | **PostgreSQL 17** | Shared DB `collabflow` for MVP |
| Cache | **Redis 8.x** | Rate limit (auth), pub/sub (realtime) |
| Messaging | **Kafka 3.9.x + ZooKeeper** (Confluent Platform **7.9.2**) | ZK learning goal; Kafka 4+ / CP 8+ are KRaft-only |
| Gateway | **Spring Cloud Gateway** | Same stack as Java services |
| API docs | **springdoc-openapi 3.1.1** | Java Swagger UI `/swagger-ui.html` |

---

## Java / Spring

| Component | Version / note |
|-----------|----------------|
| JDK | **21 LTS** (Temurin recommended) |
| Spring Boot | **4.1.1** |
| Spring Framework | Managed by Boot (**7.x**) |
| Spring Security | Managed by Boot (JWT filters + gateway OAuth2 resource server) |
| Spring Data JPA / Redis / Kafka | Managed by Boot |
| Spring Cloud BOM | **2025.1.3** |
| Spring Cloud Gateway | From Cloud BOM (`api-gateway`) |
| Flyway | Managed by Boot (per owning Java service) |
| API docs | **springdoc-openapi-starter-webmvc-ui 3.1.1** |
| JWT library | **jjwt 0.12.6** (auth / platform / task) |
| Tests | JUnit 5, Mockito, Spring Boot Test |
| Build | Multi-module **Gradle** under `java-services/` + Wrapper |
| DSL | Groovy `build.gradle` |

### Java conventions

- Language level: **21** (no preview features)
- Packaging: Boot jar
- Config: `application.yml` + **env vars** — `API_KEY` / `JWT_SECRET` from repo-root `.env` only (no secret literals in YAML). `bootRun` loads `.env` (see [running.md](running.md))
- Always commit `gradlew` / `gradlew.bat` + `gradle/wrapper/` (including `gradle-wrapper.jar`)

---

## Golang

| Component | Version / note |
|-----------|----------------|
| Go toolchain | **1.27.x** |
| `go` directive | `go 1.27` |
| HTTP | **Gin** v1.12.0 |
| ORM | **GORM** v1.31.0 + postgres driver |
| Kafka | **segmentio/kafka-go** v0.4.47 |
| Redis | **go-redis/v9** (realtime-service) |
| WebSocket | **gorilla/websocket** v1.5.3 |
| Tests | testify |

---

## Infrastructure (Docker Compose)

| Service | Image tag (local) | Notes |
|---------|-------------------|--------|
| PostgreSQL | `postgres:17-alpine` | DB name `collabflow` |
| Redis | `redis:8-alpine` | Auth rate-limit; realtime pub/sub |
| ZooKeeper | `confluentinc/cp-zookeeper:7.9.2` | Classic Kafka metadata |
| Kafka | `confluentinc/cp-kafka:7.9.2` | Apache Kafka **3.9.x** via CP 7.9 |

**Why not Kafka 4.x?** Kafka 4.0+ and CP 8.0+ **removed ZooKeeper**. MVP pins **CP 7.9.x** for ZK experience. KRaft migration is a post-MVP portfolio story.

Optional later: Mailhog, MinIO (Phase 2 — [future-plan.md](future-plan.md)).

---

## Cross-cutting

| Concern | Choice |
|---------|--------|
| API style | REST JSON, `/api/v1/...` via gateway `:8080` |
| Client gate | Header `X-API-Key` = env `API_KEY` |
| User auth | JWT access + refresh tokens (`JWT_SECRET`) |
| Migrations | Flyway in Java owners; Go uses shared schema |
| Frontend | Next.js (pin when started — not MVP) |
| Containers | Docker + Compose v2 |
| CI | Deferred |

---

## Explicitly not using (MVP)

| Avoid | Reason |
|-------|--------|
| Maven | Project standard is **Gradle** |
| Spring Boot 3.5.x | OSS EOL |
| Kafka 4.x / CP 8+ locally | No ZooKeeper |
| Echo / Fiber | One Go web stack (**Gin**) |
| Kong | Spring Cloud Gateway |
| Elasticsearch / MinIO | Phase 2+ |

---

## Upgrade policy

1. **Patch** Boot / Gradle / Go / CP images when starting a new coding phase.  
2. **Kafka → KRaft (Kafka 4 / CP 8):** deliberate migration after MVP; update this file.  
3. When versions disagree with older docs, **this file wins**.
