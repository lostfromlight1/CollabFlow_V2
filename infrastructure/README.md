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
| 5432 | PostgreSQL (`collabflow` / `collabflow` / `collabflow`) |
| 6379 | Redis |
| 2181 | ZooKeeper |
| 9092 | Kafka (clients on the host use `localhost:9092`) |

Inside the Docker network, other containers should use `kafka:29092`.

## Smoke checks

```bash
# Postgres
docker exec -it collabflow-postgres pg_isready -U collabflow

# Redis
docker exec -it collabflow-redis redis-cli ping

# Kafka — list topics (may be empty at first)
docker exec -it collabflow-kafka kafka-topics --bootstrap-server localhost:9092 --list
```

## Learning note

ZooKeeper holds Kafka cluster metadata in this classic mode. Kafka brokers register with ZK; producers/consumers talk to Kafka on `9092`, not to ZK directly.
