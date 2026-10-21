---
name: teach-and-build
description: >-
  Teach-first implementation for CollabFlow topics Kaung is learning (Golang,
  microservices, Redis, Kafka). Use when coding Go services, wiring Kafka/Redis,
  designing service boundaries, or when the user asks to learn while building /
  avoid vibe coding / explain before implementing.
---

# Teach and Build

For Kaung (junior Java, learning Go / microservices / Redis / Kafka). Portfolio-quality, no unexplained dumps.

## When to use

- Any Go service or Go file changes
- Adding Kafka producers/consumers or Redis usage
- Explaining why something is a separate microservice
- User says: learn while building, explain first, no vibe code

## Workflow

Copy and track:

```
Teach-and-build:
- [ ] Goal in one sentence (what we ship + what Kaung learns)
- [ ] Concept brief (5–10 lines, junior-friendly)
- [ ] Java analogy (map new idea to Spring Boot if possible)
- [ ] Tiny sketch (10–30 lines max) before full code
- [ ] Implement the slice
- [ ] “How to verify” (commands or curl)
- [ ] Optional: 2–3 check questions for Kaung
```

### 1. Goal

State **deliverable** and **learning outcome** (e.g. “notification-service consumes `task.assigned`; Kaung learns Kafka consumer groups”).

### 2. Concept brief

Plain language. Cover only what this slice needs:

| Topic | Teach |
|-------|--------|
| Microservice | Boundary, sync vs async, what this service owns |
| Kafka | Topic, producer after commit, consumer, idempotency |
| Redis | Key pattern, TTL, cache vs session vs pub/sub |
| Golang | Packages, `context`, Gin handler → service → repo vs Spring layers |

### 3. Java analogy

Example: “Gin handler ≈ `@RestController`; GORM ≈ Spring Data JPA; Kafka consumer ≈ `@KafkaListener`.”

### 4. Sketch then implement

- Show a minimal snippet first
- Then write real project code in small steps
- Comment **why** on unfamiliar Go/Kafka/Redis lines (not obvious Java)

### 5. Verify + check

- Give exact run/test steps Kaung can do himself
- Optional questions, e.g. “Why publish after DB commit?” / “What happens if the consumer runs twice?”

## Guardrails

- Do **not** generate an entire unfamiliar service in one opaque blob
- Prefer one vertical slice (e.g. one endpoint + one event) per teach-and-build cycle
- If scope is huge, stop and propose a smaller slice before coding
- Keep portfolio narrative: Kaung can explain architecture choices in an interview

## Related

- Architecture: `.cursor/rules/collabflow-core.mdc`
- Profile: `.cursor/rules/kaung-learning-portfolio.mdc`
- Weeks / order: `docs/implementation-guide.md`
- Check off: `docs/progress-tracker.md`
- Feature planning: skill `plan-mvp-feature`
