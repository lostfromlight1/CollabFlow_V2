# CollabFlow — Technology Stack (pinned)

**Policy:** Prefer **current OSS-supported** releases that are **stable** (no milestones/RCs). Pin majors for reproducibility; bump patch/minor deliberately.

**Last reviewed:** 2026-10-21  

Canonical services: [services-mvp.md](services-mvp.md).

---

## Decision summary

| Area | Choice | Why |
|------|--------|-----|
| Java | **21 (LTS)** | Long support, best Spring ecosystem/docs for learners; Boot 4 fully supports it |
| Spring Boot | **4.1.x** (start `4.1.1`) | Boot 3.5 is OSS EOL; 4.1.x is current stable |
| Spring Cloud | **2025.1.x** Oakwood (start `2025.1.3`) | Official train for Boot 4.0 / 4.1 |
| Build | **Gradle 8.14+** (Wrapper) | Project choice; Boot 4.1 requires Gradle 8.14+ or 9.x |
| Go | **1.27.x** (module `go 1.27`) | Current stable Go line |
| DB | **PostgreSQL 17** | Mature major (17.x) |
| Cache | **Redis 8.x** | Current stable Redis |
| Messaging | **Kafka 3.9.x + ZooKeeper** (Confluent Platform **7.9.x**) | You want ZK experience; Kafka 4+ / CP 8+ are **KRaft-only** |
| Gateway | **Spring Cloud Gateway** | Same stack as Java services |

---

## Java / Spring

| Component | Version / note |
|-----------|----------------|
| JDK | **21 LTS** (Temurin or Oracle OpenJDK 21) |
| Spring Boot | **4.1.1** (allow 4.1.x patches) |
| Spring Framework | Managed by Boot (**7.x**) |
| Spring Security | Managed by Boot (**JWT**) |
| Spring Data JPA / Redis / Kafka | Managed by Boot |
| Spring Cloud BOM | **2025.1.3** (`spring-cloud-dependencies`) |
| Spring Cloud Gateway | From Cloud BOM (`api-gateway`) |
| Flyway | Managed by Boot |
| API docs | **springdoc-openapi** (pin when first service is created) |
| Tests | JUnit 5, Mockito, Spring Boot Test |
| Build | **Gradle** + **Gradle Wrapper** in each Java service (or a multi-project build under `java-services/`) |
| DSL | **Groovy** `build.gradle` (easier for juniors); Kotlin DSL optional later |

### Java conventions

- Language level: **21** (no preview features)
- Packaging: Boot jar
- Config: `application.yml` + env vars
- Always commit `gradlew` / `gradlew.bat` + `gradle/wrapper/`

---

## Golang

| Component | Version / note |
|-----------|----------------|
| Go toolchain | **1.27.x** |
| `go` directive | `go 1.27` |
| HTTP | **Gin** v1.12.x |
| ORM | **GORM** v1.31.x + postgres driver |
| Kafka | **segmentio/kafka-go** v0.4.x |
| Redis | **go-redis/v9** |
| WebSocket | gorilla/websocket |
| Tests | testify |

---

## Infrastructure (Docker Compose)

| Service | Image tag (local dev) | Notes |
|---------|----------------------|--------|
| PostgreSQL | `postgres:17-alpine` | DB name `collabflow` |
| Redis | `redis:8-alpine` | Sessions, cache, pub/sub |
| ZooKeeper | `confluentinc/cp-zookeeper:7.9.2` | Metadata for Kafka (classic mode) |
| Kafka | `confluentinc/cp-kafka:7.9.2` | Apache Kafka **3.9.x** via Confluent Platform 7.9 |

**Why not Kafka 4.x?** Kafka 4.0+ and Confluent Platform 8.0+ **removed ZooKeeper**. To learn and run ZK + Kafka together, MVP pins **CP 7.9.x** (last line with ZK; community support through ~Feb 2027). A later migration to KRaft can be a portfolio “upgrade” story.

Optional later: Mailhog, MinIO (Phase 2).

---

## Cross-cutting

| Concern | Choice |
|---------|--------|
| API style | REST JSON, `/api/v1/...` |
| Auth | JWT + refresh tokens |
| Migrations | Flyway in Java owners |
| Frontend (later) | Next.js (pin when started) |
| Containers | Docker + Compose v2 |
| CI (later) | GitHub Actions |

---

## Explicitly not using (for MVP)

| Avoid | Reason |
|-------|--------|
| Maven | Project standard is **Gradle** |
| Spring Boot 3.5.x | OSS EOL |
| Kafka 4.x / CP 8+ locally | No ZooKeeper; conflicts with learning goal |
| KRaft-only local stack | Deferred until after ZK fundamentals |
| Echo / Fiber + Gin | One Go web stack |
| Kong (MVP) | Spring Cloud Gateway |

---

## Upgrade policy

1. **Patch** Boot/Gradle/Go/CP images when starting a coding phase.  
2. **Kafka → KRaft (Kafka 4 / CP 8):** deliberate migration task after MVP; update this file.  
3. When versions disagree with older docs, **this file wins**.
