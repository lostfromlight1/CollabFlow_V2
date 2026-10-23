---
name: add-frontend-page
description: >-
  Scaffold or extend a CollabFlow Next.js App Router page with API client wiring,
  session guards, and shadcn forms. Use when adding a frontend route, admin form,
  login/org-picker screen, or when the user asks to build a Next.js page against
  the gateway.
---

# Add Frontend Page

## When to use

Building or extending something under `frontend/app/` that talks to the CollabFlow gateway.

## Prerequisites

1. Prefer running `plan-frontend-feature` first (or confirm ApiIds already exist).
2. Stack rule: [frontend-next.mdc](../../rules/frontend-next.mdc).
3. Contract: [docs/fe-contract.md](../../../docs/fe-contract.md).

## Workflow

```
Page progress:
- [ ] Route file under correct group (auth | admin | app)
- [ ] Guard in layout or page (redirect if wrong role)
- [ ] Types + lib/api/* function for the ApiId(s)
- [ ] Form or view with RHF + Zod when mutating
- [ ] Error/success UX from ApiResponse.message
- [ ] README or fe-contract touch if env/headers changed
```

### 1. Place the route

| Audience | Put under |
|----------|-----------|
| Public auth | `app/(auth)/…` |
| Master admin | `app/(admin)/admin/…` |
| Member product | `app/(app)/…` |

Keep URL without the group name (`/login`, `/admin/users`).

### 2. API module

Add or extend `frontend/lib/api/*.ts` using the shared client:

- Always send `X-API-Key`
- Attach Bearer from session when required
- Attach `X-Organization-Id` only when the contract says so
- Parse `{ success, message, data, timestamp }`; surface `message` on failure

### 3. Guard

- **admin:** session exists and `user.masterAdmin === true` → else `/login` or `/home`
- **app:** session exists → else `/login`
- **auth:** if already logged in, redirect away from login

### 4. UI

- Use existing shadcn components under `components/ui`
- Mutations: React Hook Form + Zod
- Show created ids (e.g. new user id) so the next admin step can paste them

### 5. Teach-light (Kaung)

For unfamiliar Next patterns (App Router server vs client components, `"use client"`), give a short why → tiny example → then the real file. Do not dump a whole app unprompted.

## Done when

- Page loads against gateway `:8080` with correct headers
- Wrong role cannot open the page
- No new undocumented endpoints
