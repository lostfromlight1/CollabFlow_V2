# Go services

Go **1.27**, **Gin**, **GORM**, **kafka-go**, **go-redis**. Versions: [docs/tech-stack.md](../docs/tech-stack.md).

| Service | Port | Role |
|---------|------|------|
| [collab-service](collab-service/) | 9081 | Notifications, invitations, comments, activity |
| [realtime-service](realtime-service/) | 9082 | WebSocket + Redis pub/sub |

**Conventions:** see `.cursor/rules/golang-services.mdc`.  
**Learning:** use skill `teach-and-build` when implementing these (Week 9+).

Phase C started: `collab-service` Week 9 (health skeleton first).
