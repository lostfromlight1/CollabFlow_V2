# Infrastructure (local)

Docker Compose for **Postgres 17**, **Redis 8**, **ZooKeeper + Kafka** (Confluent Platform **7.9.2**), **MailHog**.

Pins: [docs/tech-stack.md](../../docs/tech-stack.md) · Install tools: [docs/setup.md](../../docs/setup.md) · **Run apps:** [docs/running.md](../../docs/running.md)

## Prerequisites

- Docker Desktop running (`docker version` shows **Server**)
- Repo-root `.env`: `Copy-Item .env.example .env` (or `cp`)

## Commands

From **`backend/infrastructure/`**:

```bash
docker compose --env-file ../../.env up -d
docker compose --env-file ../../.env ps
docker compose --env-file ../../.env logs -f kafka
docker compose --env-file ../../.env stop
docker compose --env-file ../../.env down
docker compose --env-file ../../.env down -v   # wipe volumes
```

If `../../.env` is missing, compose still works with defaults from `docker-compose.yml`.

## Ports (host)

| Port | Service |
|------|---------|
| 5432 | PostgreSQL — `POSTGRES_*` from repo-root `.env` |
| 6379 | Redis |
| 2181 | ZooKeeper |
| 9092 | Kafka (`localhost:9092` from host apps) |
| 1025 | MailHog SMTP |
| 8025 | MailHog UI — http://localhost:8025 |

Inside the Docker network, other containers use `kafka:29092`.

**Changing `POSTGRES_USER` / password after first start:** only applies on an empty volume → `down -v` then `up -d`.
