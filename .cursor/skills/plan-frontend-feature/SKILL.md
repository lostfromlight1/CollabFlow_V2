---
name: plan-frontend-feature
description: >-
  Plan a CollabFlow frontend feature by mapping UI to fe-contract headers,
  ApiIds, routes, and auth/org guards before coding. Use when starting a Next.js
  page or flow, scoping FE work, or when the user asks for a frontend
  implementation plan.
---

# Plan Frontend Feature

## Before you start

1. Read [docs/fe-contract.md](../../../docs/fe-contract.md) (headers, auth→org flow, out-of-scope).
2. Resolve endpoints via [docs/services-mvp.md](../../../docs/services-mvp.md) ApiIds — **do not invent**.
3. Confirm the FE slice (auth + admin first; product board later).
4. Note repo layout: `frontend/` app; backend under `backend/` (or legacy `java-services/` until moved).

## Checklist

```
FE feature plan:
- [ ] User story (admin vs member)
- [ ] Routes / pages (App Router paths)
- [ ] ApiIds + methods + paths
- [ ] Required headers (API key / Bearer / org)
- [ ] Guards (anonymous / authenticated / masterAdmin)
- [ ] Session fields touched
- [ ] Out of scope for this slice
- [ ] Implementation order (smallest vertical slice)
```

## Steps

1. **Audience** — Master admin (`/admin`) vs member (`/home` / product) vs public (`/login`).
2. **API map** — List ApiIds from services-mvp; flag gaps (update docs before inventing).
3. **Headers** — Which calls need `X-Organization-Id`?
4. **UI surface** — Pages + key components; reuse shadcn; avoid new design systems.
5. **Order** — Auth/session before pages that need tokens; create-user before attach-member.
6. **Output** — Deliver the template; do **not** start coding unless asked.

## Output template

```markdown
## FE feature: {name}
**Audience:** admin | member | public
**Routes:** …

### APIs
| ApiId | Method | Path | Headers |
|-------|--------|------|---------|
| … | … | … | … |

### Guards
- …

### Session
- …

### Out of scope
- …

### Implementation checklist
1. …
2. …
```

## Guardrails

- Documents / MinIO UI: refuse for UAT unless docs change.
- Prefer `/users/me` over auth `/me`.
- If backend endpoint missing, stop and plan a backend change + doc update first.
