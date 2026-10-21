# Infrastructure (local)

Docker Compose for **Postgres 17**, **Redis 8**, **ZooKeeper + Kafka** (Confluent Platform **7.9.2**).

Pins: [docs/tech-stack.md](../docs/tech-stack.md) · Setup: [docs/setup.md](../docs/setup.md)

## Prerequisites

- Docker Desktop running (`docker version` shows **Server**)
- Copy env file once: `cp ../.env.example ../.env` (Windows: copy manually)

## Commands

From **`infrastructure/`**:

```bash
# Start
docker compose --env-file ../.env up -d

# Status
docker compose --env-file ../.env ps

# Logs
docker compose --env-file ../.env logs -f kafka

# Stop (keep volumes)
docker compose --env-file ../.env stop

# Remove containers (keep volumes)
docker compose --env-file ../.env down

# Wipe data volumes too
docker compose --env-file ../.env down -v
```

If `../.env` is missing, compose still works with defaults from `docker-compose.yml`.

## Ports (host)

| Port | Service |
|------|---------|
| 5432 | PostgreSQL — DB/user/password from `../.env` (`POSTGRES_*`) |
| 6379 | Redis |
| 2181 | ZooKeeper |
| 9092 | Kafka (host clients: `localhost:9092`) |

Example local creds (see your `.env`): DB `collabflow`, user `postgres`, password `root`.

Inside the Docker network, other containers should use `kafka:29092`.

**Changing `POSTGRES_USER` / password after the first start:** Postgres only applies these on an empty volume. Reset with:

```bash
docker compose --env-file ../.env down -v
docker compose --env-file ../.env up -d
```

## Smoke checks

```bash
# Postgres (use the same user as POSTGRES_USER in .env)
docker exec -it collabflow-postgres pg_isready -U postgres

# Redis
docker exec -it collabflow-redis redis-cli ping

# Kafka — list topics (may be empty at first)
docker exec -it collabflow-kafka kafka-topics --bootstrap-server localhost:9092 --list
```

JDBC URL for apps later:

```text
jdbc:postgresql://localhost:5432/collabflow
# user=postgres  password=root  (from .env)
```

## Learning note

ZooKeeper holds Kafka cluster metadata in this classic mode. Kafka brokers register with ZK; producers/consumers talk to Kafka on `9092`, not to ZK directly.
