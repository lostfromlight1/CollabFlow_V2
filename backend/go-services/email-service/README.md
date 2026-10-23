# email-service

**Port:** 9084 · Go **1.27** / Gin / kafka-go / `net/smtp`  
**Phase:** Wave 8 — **real SMTP** (local MailHog or provider)  
**Role:** Consume Kafka `email.send` and deliver email  

Waves: [phase-2.md](../../../docs/phase-2.md) · Envelope: [microservice.md](../../../docs/microservice.md)

### See also / Depends on

| Topic | Owner |
|-------|--------|
| Produce `email.send` | [auth-service](../../java-services/auth-service/README.md), collab invites |
| Local SMTP catcher | Docker **MailHog** — UI `:8025`, SMTP `:1025` |
| Disable collab stub | `EMAIL_SERVICE_ENABLED=true` on collab |

## What / why

Auth/collab publish `email.send` after DB commit. This service dials SMTP and sends. Locally use **MailHog** (no auth/TLS). In prod set `SMTP_USER` / `SMTP_PASSWORD` and usually `SMTP_TLS=true` on port 587.

### Coexistence

| Mode | Behavior |
|------|----------|
| `EMAIL_SERVICE_ENABLED=false` (collab) | Collab stub **and** email-service both get the topic (different groups) |
| `EMAIL_SERVICE_ENABLED=true` | Collab skips email consumer; **only email-service** sends |

## Config (env)

| Variable | Local default | Purpose |
|----------|---------------|---------|
| `EMAIL_HTTP_PORT` | `9084` | Health |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Brokers |
| `EMAIL_KAFKA_GROUP_ID` | `email-service` | Consumer group |
| `SMTP_HOST` | `localhost` | MailHog / provider |
| `SMTP_PORT` | `1025` | MailHog SMTP (prod often `587`) |
| `SMTP_TLS` | `false` | `true` for STARTTLS providers |
| `SMTP_USER` / `SMTP_PASSWORD` | empty | Required for most providers |
| `SMTP_FROM` | `noreply@collabflow.local` | From |

## Run

```powershell
# Infra (includes MailHog)
cd D:\codes\CollabFlow_V2\backend\infrastructure
docker compose --env-file ../../.env up -d

cd D:\codes\CollabFlow_V2\backend\go-services\email-service
go run ./cmd/server

# Trigger verify/reset/invite → open http://localhost:8025
```

## Learning note

**Why Kafka → SMTP?** The API returns immediately after the DB write; email can fail/retry without blocking login. MailHog lets you learn the path without a real mailbox.
