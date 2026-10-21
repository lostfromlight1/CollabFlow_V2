# CollabFlow

Multi-tenant collaboration and project management (org → workspace → project → task).

**Status:** Docs finalized for slim MVP — coding not started. Track progress in [docs/progress-tracker.md](docs/progress-tracker.md).

## Scope

**6 services** (not 16). Versions: [docs/tech-stack.md](docs/tech-stack.md).

| Service | Stack | Responsibility |
|---------|--------|----------------|
| auth-service | Java 21 / Boot 4.1 / Gradle | Users, JWT |
| platform-service | Java 21 / Boot 4.1 / Gradle | Org + workspace + project |
| task-service | Java 21 / Boot 4.1 / Gradle | Tasks + workflows |
| api-gateway | Spring Cloud Gateway / Gradle | Edge routing |
| collab-service | Go 1.27 / Gin | Comments, notifications, invitations, activity |
| realtime-service | Go 1.27 | WebSocket + Redis |

Infra: Postgres 17, Redis 8, Kafka 3.9 + ZooKeeper (Confluent 7.9).

## Start here

| You want… | Open |
|-----------|------|
| **Setup PC (Windows / Fedora)** | [docs/setup.md](docs/setup.md) |
| Week-by-week plan | [docs/implementation-guide.md](docs/implementation-guide.md) |
| Checkbox tracker | [docs/progress-tracker.md](docs/progress-tracker.md) |
| Doc map | [docs/index.md](docs/index.md) |
| Cursor AI guide | [AGENTS.md](AGENTS.md) |

## Documentation

| Doc | Contents |
|-----|----------|
| [docs/implementation-guide.md](docs/implementation-guide.md) | **Build order + weeks** |
| [docs/progress-tracker.md](docs/progress-tracker.md) | **What’s finished** |
| [docs/services-mvp.md](docs/services-mvp.md) | Service boundaries |
| [docs/tech-stack.md](docs/tech-stack.md) | Pinned versions |
| [docs/future-plan.md](docs/future-plan.md) | Deferred services |
| [docs/database.md](docs/database.md) | Schema + ownership |
| [docs/microservice.md](docs/microservice.md) | Endpoint/event reference |

## Cursor

- Rules: [`.cursor/rules/`](.cursor/rules/)  
- Skills: [`.cursor/skills/`](.cursor/skills/) — e.g. `teach-and-build`, `plan-mvp-feature`  

## Repo layout

```
java-services/     auth, platform, task, api-gateway
go-services/       collab, realtime
infrastructure/    docker-compose (Postgres, Redis, ZK, Kafka)
docs/              guides + schema
```

## Next step

1. Tools: [docs/setup.md](docs/setup.md)  
2. Copy `.env.example` → `.env`, start infra:

```bash
cd infrastructure
docker compose --env-file ../.env up -d
```

3. Week 2: Gradle service skeletons ([docs/implementation-guide.md](docs/implementation-guide.md)).
