# CollabFlow frontend

Next.js **App Router** + React + TypeScript + Tailwind + shadcn/ui + framer-motion + lucide-react.

**Contract:** [docs/fe-contract.md](../docs/fe-contract.md)  
**Backend runbook:** [docs/running.md](../docs/running.md) · [backend/README.md](../backend/README.md)

## Setup

```powershell
cd frontend
Copy-Item .env.example .env.local
# Set NEXT_PUBLIC_API_KEY to the same value as repo-root .env API_KEY
npm install
npm run dev
```

App: http://localhost:3000 → gateway http://localhost:8080

## Required backend (auth + admin UI)

1. Infra (`backend/infrastructure`) + MailHog optional for this slice  
2. **auth** `:8081` · **user** `:8084` · **platform** `:8082` · **api-gateway** `:8080`  
3. `.env` at repo root with `API_KEY`, `JWT_SECRET`, `MASTER_ADMIN_*`, `CORS_ALLOWED_ORIGINS=http://localhost:3000`

## Routes

| Path | Who |
|------|-----|
| `/` | Public landing (session-aware CTA) |
| `/login` | Public |
| `/select-org` | Authenticated (multi-org) |
| `/verify-email` | Public (register flow) |
| `/admin`, `/admin/users`, `/admin/organizations` | `masterAdmin` |
| `/home` | Authenticated member home |
| `/register`, `/forgot-password`, `/reset-password` | Public auth |
| `/workspaces`, `/workspaces/[id]`, `/org`, `/activity`, `/notifications` | Member product (needs org) |
| `/projects/[id]/*` | Project home, tasks, board, sprints, documents |

## Theme (brighter nature greens)

CSS variables in `app/globals.css` (`:root`):

| Token | Role |
|-------|------|
| `--background` | Calm mist paper (light surfaces) |
| `--primary` | **Leaf** — livelier primary actions |
| `--secondary` / `--accent` | Soft mint washes |
| `--sidebar` | **Moss** wash (distinct from canvas) |
| `--chart-1`…`5` | Leaf → moss → mint spectrum |
| `--badge-success` / `--badge-info` / `--badge-warn` | Generic badges / Demo |

### Status chips (`--status-*`)

Soft tinted backgrounds + readable foregrounds. Classes: `chip-status-todo|progress|review|done|blocked`. Components: `StatusChip` + `lib/task-meta.ts`.

| Status | Hue |
|--------|-----|
| To Do | slate/blue |
| In Progress | amber |
| In Review | violet |
| Done | green |
| Blocked | red |

### Priority chips (`--priority-*`)

Distinct from status. Classes: `chip-priority-low|medium|high|urgent`. Component: `PriorityChip` (icons: down / minus / up / flame).

| Priority | Hue |
|----------|-----|
| Low | teal |
| Medium | yellow |
| High | orange |
| Urgent | rose/red |

### Role chips (`--role-*`)

| Role | Hue |
|------|-----|
| Owner | gold / amber |
| Admin | sky / blue |
| Member | mint / sage |

Light is default; Theme in the sidebar user menu toggles `.dark`. Prefer tokens over hardcoding colors.

## Icons

**Set:** `lucide-react` only via `components/ui/icon.tsx`.

| Context | Size |
|---------|------|
| Nav / buttons / fields | `size-4` (`md`) |
| PageHeader | `size-5` (`lg`) |
| Empty state | `size-10` (`xl`) |
| Dense badge | `size-3.5` (`sm`) |

Icons inherit parent `interactive-*` hover/press. Decorative icons use `aria-hidden`. Real-route map: `lib/icons.ts`.

## Motion system

**Single source:** `lib/motion/tokens.ts` + `--motion-*` in `globals.css`.  
**Framer:** `lib/motion/variants.ts` + `components/motion/*`.  
**Skeletons:** delayed gate in `components/feedback/skeleton.tsx` (~180ms).

| Class | Behavior |
|-------|----------|
| `interactive-press` | scale 0.98 |
| `interactive-hover-fill` | fill shift (buttons/nav) |
| `interactive-hover-lift` | cards: lift + shadow, **no scale** |
| `interactive-hover-row` | tables: tint only |
| `interactive-hover-link` | color → primary |
| `interactive-focus` / `interactive-input` | focus ring / input border |

Enter-only page transitions; stagger first **8** items; `useReducedMotion` → reduced variant.

## UI kit (shared)

| Area | Path |
|------|------|
| Shells | `components/layout/*` |
| Data | `components/data/*` |
| Feedback | `components/feedback/*` |
| Forms | `components/forms/form-field.tsx` |
| Icon / Badge / chips | `components/ui/icon.tsx`, `badge.tsx`, `status-chip.tsx`, `priority-chip.tsx`, `role-chip.tsx` |
| Scrollbars | Soft sage thumbs via `--scrollbar-*`; use `scroll-area` on overflow regions — no default browser chrome |
| User menu | `components/layout/user-menu.tsx` (avatar · Profile / Settings / Switch org / Theme / Log out) |

## Smoke

1. Open `/` → Sign in
2. Login as master admin → users, organizations, transfer ownership
3. Log out → login as member → org picker / home → workspaces → project board
4. Profile and security live under `/settings/profile` and `/settings/security`
