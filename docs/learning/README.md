# CollabFlow learning notes

This folder explains how the running system fits together. It is written from the code and from the contracts in `docs/`. It is a reading guide, not a second source of truth.

When a table, route, or topic changes, update `docs/services-mvp.md`, `docs/database.md`, and `docs/tech-stack.md` in the same change. Then adjust the chapter here that describes that piece.

## Reading order

Read `00` through `05` first. The flow chapters (`06`–`12`) stand on their own once those are familiar. Finish with cross-cutting rules and where to look when you change something.

| Order | File | What you get |
|------:|------|----------------|
| 0 | [00-overview.md](00-overview.md) | What CollabFlow is, the hierarchy, and the nine processes |
| 1 | [01-project-structure.md](01-project-structure.md) | Folders, ports, build tools, and how to start the stack |
| 2 | [02-data-stores.md](02-data-stores.md) | Shared Postgres, Redis, and Kafka topics |
| 3 | [03-data-model.md](03-data-model.md) | Tables, ownership, and relationships |
| 4 | [04-apis.md](04-apis.md) | Gateway routes, headers, and ApiIds |
| 5 | [05-layer-connections.md](05-layer-connections.md) | Browser to gateway to service, and Kafka fan-out |
| 6 | [06-flow-auth.md](06-flow-auth.md) | Login, refresh, email verify, org select |
| 7 | [07-flow-tenancy.md](07-flow-tenancy.md) | Organization, workspace, project, roles |
| 8 | [08-flow-tasks.md](08-flow-tasks.md) | Tasks, status, assign, sprints, events |
| 9 | [09-flow-collab.md](09-flow-collab.md) | Comments, notifications, invitations |
| 10 | [10-flow-realtime.md](10-flow-realtime.md) | WebSocket rooms and Redis pub/sub |
| 11 | [11-flow-documents.md](11-flow-documents.md) | Upload, list, and download |
| 12 | [12-flow-admin.md](12-flow-admin.md) | Master admin, audit, flags, impersonation |
| 13 | [13-cross-cutting.md](13-cross-cutting.md) | Security, envelopes, logs, disagreements |
| 14 | [14-working-in-the-project.md](14-working-in-the-project.md) | Where to look and what not to break |

## How to read a claim

- A class or file name means the statement was checked in that file.
- Existing docs (`docs/services-mvp.md`, `docs/database.md`, `docs/tech-stack.md`, `docs/fe-contract.md`, `docs/running.md`) are preferred when they match the code.
- When they disagree, the chapter says so and follows the code.
- An **Unverified** section lists anything that was not opened.

## Canonical docs (do not replace these)

| Need | File |
|------|------|
| Services, ports, ApiIds | [services-mvp.md](../services-mvp.md) |
| Schema | [database.md](../database.md) |
| Versions | [tech-stack.md](../tech-stack.md) |
| Browser contract | [fe-contract.md](../fe-contract.md) |
| How to run | [running.md](../running.md) |
| Agent map | [AGENTS.md](../../AGENTS.md) |
