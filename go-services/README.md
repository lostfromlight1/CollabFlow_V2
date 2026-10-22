# Go services

Go **1.27**, **Gin**, **GORM**, **kafka-go**, **go-redis**. Versions: [docs/tech-stack.md](../docs/tech-stack.md).

**MVP:** Complete (Weeks 9–12). **How to run (incl. loading `.env`):** [docs/running.md](../docs/running.md).

| Service | Port | Role |
|---------|------|------|
| [collab-service](collab-service/) | 9081 | Notifications, invitations, comments, activity |
| [realtime-service](realtime-service/) | 9082 | WebSocket + Redis pub/sub + Kafka |
| [document-service](document-service/) | 9083 | Documents stubs + MinIO env |
| [email-service](email-service/) | 9084 | `email.send` → real SMTP (MailHog locally) |

**Conventions:** `.cursor/rules/golang-services.mdc`.  
**Learning:** skill `teach-and-build` when changing these services.

Java produces Kafka; Go owns async side effects, live push, and Phase 2 email/documents.
