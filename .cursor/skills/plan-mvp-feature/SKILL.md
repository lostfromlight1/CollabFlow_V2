---
name: plan-mvp-feature
description: >-
  Plan implementation of a CollabFlow feature by mapping it to services, tables,
  endpoints, and Kafka events. Use when starting an MVP feature from feature.md,
  scoping work before coding, or when the user asks for a feature implementation plan.
---

# Plan MVP Feature

## Before you start

1. Confirm the feature appears in the MVP order in [docs/implementation-guide.md](../../../docs/implementation-guide.md).
2. Open [project_overview/feature.md](../../../project_overview/feature.md) for detail.
3. Map to a **slim** service in [docs/services-mvp.md](../../../docs/services-mvp.md).
4. Cross-check [docs/microservice.md](../../../docs/microservice.md) + [docs/database.md](../../../docs/database.md).
5. Remind the user to check off [docs/progress-tracker.md](../../../docs/progress-tracker.md) when done.

## Checklist

```
Feature plan:
- [ ] Feature name + phase (MVP / 2 / 3 / 4)
- [ ] Owning service(s) + language
- [ ] Tables involved
- [ ] REST endpoints
- [ ] Kafka events (produce / consume)
- [ ] Redis usage (if any)
- [ ] Dependencies on other features/services
- [ ] Implementation checklist (ordered tasks)
```

## Steps

1. **Identify** — Feature section in feature.md (description, service, complexity, dependencies).
2. **Services** — List Java/Go owners; note cross-service flows (e.g. assign task → notification + activity + realtime).
3. **Data** — Tables and key columns from database.md; note FKs and permission checks.
4. **API** — Endpoints from microservice.md; flag gaps to document before inventing.
5. **Events** — Produce/consume list; if missing, plan an `add-kafka-event` follow-up.
6. **Order** — Sequence tasks so dependencies exist first (e.g. users before org members).
7. **Output** — Deliver the template below to the user; do not start coding unless asked.

## Output template

```markdown
## Feature: {name}
**Phase:** MVP | 2 | 3 | 4
**Primary service:** {name} ({Java|Go})
**Also involves:** {services}

### Tables
- …

### Endpoints
- …

### Kafka
- Produce: …
- Consume: …

### Redis
- …

### Dependencies
- …

### Implementation checklist
1. …
2. …
3. …
```

## Guardrails

- If the feature is not MVP, say so and ask whether to proceed or stick to Phase 1.
- Do not invent schema or routes; use `add-flyway-migration` / doc updates when gaps are intentional.
- Prefer the smallest vertical slice that demos the feature end-to-end.
