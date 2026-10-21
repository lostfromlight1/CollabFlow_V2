---
name: add-flyway-migration
description: >-
  Add a Flyway SQL migration for CollabFlow PostgreSQL and keep docs/database.md
  in sync. Use when changing schema, adding tables/columns/indexes, or when the
  user asks for a database migration.
---

# Add Flyway Migration

## Before you start

1. Read the target tables in [docs/database.md](../../../docs/database.md).
2. Confirm which service owns the tables (microservice.md Data Management).
3. Shared DB MVP: migrations may live in the owning Java service or a shared migrations folder once it exists — stay consistent with the repo.

## Conventions

- Naming: `V{version}__{snake_case_description}.sql`
- Types: UUID PKs, TIMESTAMPTZ, CHECKs and FKs as in database.md
- Follow `.cursor/rules/database-postgres.mdc`

## Checklist

```
Migration:
- [ ] Next version number chosen (no gaps/reuse)
- [ ] SQL matches database.md (or database.md updated first)
- [ ] Indexes and unique constraints included
- [ ] Seed data only for system roles/permissions/workflows if required
- [ ] docs/database.md updated (columns, indexes, relationships)
- [ ] Rollback note (manual or down script) documented if risky
```

## Steps

1. **Design** — Edit or confirm the table section in database.md.
2. **Write SQL** — Create `V{n}__....sql` under the service’s `db/migration/` path.
3. **Seeds** — System roles, permissions, default workflow: follow Seed Data in database.md; prefer idempotent inserts where possible.
4. **Verify** — Column names, FK targets, ON DELETE behavior match docs.
5. **Cross-check** — No undocumented tables; analytics/export remain read-only consumers.

## Example stub

```sql
-- V4__create_workspaces.sql
CREATE TABLE workspaces (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    -- ... remaining columns from docs/database.md
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_workspaces_organization_id ON workspaces(organization_id);
```

## Do not

- Ship a migration without updating database.md
- Use `TIMESTAMP` instead of `TIMESTAMPTZ` for domain times
