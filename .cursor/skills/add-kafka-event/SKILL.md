---
name: add-kafka-event
description: >-
  Define and wire a CollabFlow Kafka domain event (topic, JSON schema, producer,
  consumers). Use when adding events like task.assigned, updating event catalogs,
  or when the user mentions new Kafka topics or event-driven side effects.
---

# Add Kafka Event

## Before you start

1. Check existing topics in [docs/microservice.md](../../../docs/microservice.md) (Kafka Event Streaming).
2. Prefer documented events; if new, update microservice.md in the same change.
3. Events are for **side effects** (notifications, activity, search) — not for replacing required sync reads.

## Event envelope

```json
{
  "eventId": "uuid",
  "eventType": "task.assigned",
  "timestamp": "2026-10-21T10:00:00Z",
  "version": "1.0",
  "data": {}
}
```

## Checklist

```
Kafka event:
- [ ] Topic name chosen (dot notation, e.g. entity.action)
- [ ] Envelope + data fields documented
- [ ] Producer service identified (writes the aggregate)
- [ ] Consumer services listed (notification, activity, search, …)
- [ ] Publish-after-commit in producer
- [ ] Idempotent consume (dedupe by eventId or business key)
- [ ] microservice.md updated (topics + who produces/consumes)
```

## Steps

1. **Name** — `{entity}.{action}` (e.g. `project.member_added`). Align with existing list.
2. **Schema** — Document `data` fields needed by every consumer; avoid oversized payloads (IDs + minimal context).
3. **Producer** — Emit only after successful DB write in the owning service.
4. **Consumers** — For each consumer: handler, retry/DLQ note, no duplicate side effects.
5. **Docs** — Update microservice.md Core Events / service Kafka Topics sections.
6. **Shared types** — When `shared/` libraries exist, add event structs in Java and Go; until then, document JSON only.

## Do not

- Use Kafka for request/response that the API caller needs immediately
- Change event `version` without documenting compatibility
