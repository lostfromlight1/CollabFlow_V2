# CollabFlow Documentation Index

Short map of project docs. **Start with the bold rows.**

## Canonical (use these)

| Question | Doc |
|----------|-----|
| **PC setup (Windows / Fedora)** | [setup.md](setup.md) |
| **Week-by-week build + feature order** | [implementation-guide.md](implementation-guide.md) |
| **What is finished?** (checkboxes) | [progress-tracker.md](progress-tracker.md) |
| **Which services?** | [services-mvp.md](services-mvp.md) |
| **Pinned versions** | [tech-stack.md](tech-stack.md) |
| **Deferred / reduced services** | [future-plan.md](future-plan.md) |
| Tables / ownership | [database.md](database.md) |
| Cursor / agents | [../AGENTS.md](../AGENTS.md) |

## Reference (deep / legacy)

| Question | Doc |
|----------|-----|
| Feature catalog / phases | [../project_overview/feature.md](../project_overview/feature.md) |
| Endpoint & Kafka event detail | [microservice.md](microservice.md) |
| Old 16-service week plan | [../project_overview/implementation.md](../project_overview/implementation.md) (**do not follow**) |

## Slim MVP services (6)

| Service | Lang | Role |
|---------|------|------|
| auth-service | Java | Users, JWT |
| platform-service | Java | Org + workspace + project |
| task-service | Java | Tasks + workflows |
| api-gateway | Java | Edge routing |
| collab-service | Go | Comments, notifications, invitations, activity |
| realtime-service | Go | WebSocket + Redis |

## Build order (summary)

1. Infra (Docker: Postgres, Redis, ZK, Kafka)  
2. Gateway skeleton  
3. auth → platform → task (Java)  
4. collab → realtime (Go)  
5. Basic analytics + E2E polish  

## Hierarchy

```
Organization → Workspace → Project → Task
```

## Cursor artifacts

- Rules: `../.cursor/rules/` (`collabflow-core`, `kaung-learning-portfolio`, java/go/db)  
- Skills: `../.cursor/skills/` (`teach-and-build`, `plan-mvp-feature`, …)  
- Guide: `../AGENTS.md`
