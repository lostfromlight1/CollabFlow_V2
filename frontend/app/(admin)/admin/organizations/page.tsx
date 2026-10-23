"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { PageHeader } from "@/components/layout/page-header";
import { InlineAlert } from "@/components/feedback/inline-alert";
import { DelayedSkeletonGate, ListSkeleton } from "@/components/feedback/skeleton";
import { DataTable, type DataColumn } from "@/components/data/data-table";
import { Dialog } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { FormField } from "@/components/forms/form-field";
import {
  adminCreateOrganization,
  downloadAdminCsv,
  listAdminOrganizations,
  type AdminOrganization,
} from "@/lib/api/admin";
import { ApiError } from "@/lib/api/client";
import { Icons } from "@/lib/icons";

const fieldClass = "h-8 w-full rounded-lg border border-input bg-background px-2 text-sm";

function when(value: string | null | undefined) {
  if (!value) return "—";
  return value.slice(0, 16).replace("T", " ");
}

export default function AdminOrganizationsPage() {
  const [rows, setRows] = useState<AdminOrganization[]>([]);
  const [page, setPage] = useState(0);
  const [total, setTotal] = useState(0);
  const [sort, setSort] = useState("createdAt,desc");
  const [q, setQ] = useState("");
  const [active, setActive] = useState("");
  const [plan, setPlan] = useState("");
  const [createdFrom, setCreatedFrom] = useState("");
  const [createdTo, setCreatedTo] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [open, setOpen] = useState(false);
  const [name, setName] = useState("");
  const [slug, setSlug] = useState("");
  const [description, setDescription] = useState("");
  const [ownerUserId, setOwnerUserId] = useState("");

  const query = {
    q,
    active: active === "" ? undefined : active === "true",
    plan: plan || undefined,
    createdFrom: createdFrom ? `${createdFrom}T00:00:00.000Z` : undefined,
    createdTo: createdTo ? `${createdTo}T23:59:59.999Z` : undefined,
    page,
    size: 20,
    sort,
  };

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    listAdminOrganizations(query)
      .then((result) => {
        if (cancelled) return;
        setRows(result.content ?? []);
        setTotal(result.totalElements ?? 0);
        setError(null);
      })
      .catch((e) => {
        if (!cancelled) setError(e instanceof ApiError ? e.message : "Could not load organizations");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [q, active, plan, createdFrom, createdTo, page, sort]);

  function toggleSort(field: string) {
    setPage(0);
    setSort((current) => (current === `${field},asc` ? `${field},desc` : `${field},asc`));
  }

  const columns: DataColumn<AdminOrganization>[] = [
    {
      id: "name",
      header: <button type="button" onClick={() => toggleSort("name")}>Name</button>,
      cell: (row) => (
        <Link href={`/admin/organizations/${row.id}`} className="font-medium text-primary">{row.name}</Link>
      ),
    },
    { id: "slug", header: <button type="button" onClick={() => toggleSort("slug")}>Slug</button>, cell: (row) => row.slug },
    { id: "owner", header: "Owner", cell: (row) => row.ownerName || row.ownerEmail || row.ownerId },
    {
      id: "status",
      header: "Status",
      cell: (row) => <Badge variant={row.active ? "success" : "secondary"}>{row.active ? "Active" : "Suspended"}</Badge>,
    },
    { id: "plan", header: "Plan", cell: (row) => row.plan },
    { id: "created", header: <button type="button" onClick={() => toggleSort("createdAt")}>Created</button>, cell: (row) => when(row.createdAt) },
  ];

  async function createOrg() {
    await adminCreateOrganization({ name, slug, description: description || undefined, ownerUserId });
    setOpen(false);
    setName("");
    setSlug("");
    setDescription("");
    setOwnerUserId("");
    const result = await listAdminOrganizations({ ...query, page: 0 });
    setPage(0);
    setRows(result.content ?? []);
    setTotal(result.totalElements ?? 0);
  }

  return (
    <div className="space-y-4">
      <PageHeader
        title="Organizations"
        description="Every organization, including suspended ones."
        icon={Icons.organizations}
        actions={
          <div className="flex gap-2">
            <Button
              type="button"
              variant="outline"
              onClick={() =>
                downloadAdminCsv(`/api/v1/admin/organizations/export${exportQuery(query)}`, "organizations.csv").catch((e) =>
                  setError(e instanceof Error ? e.message : "Export failed")
                )
              }
            >
              Export CSV
            </Button>
            <Button type="button" onClick={() => setOpen(true)}>Create organization</Button>
          </div>
        }
      />
      {error ? <InlineAlert>{error}</InlineAlert> : null}
      <div className="grid gap-2 sm:grid-cols-4">
        <Input placeholder="Search name or slug" value={q} onChange={(e) => { setPage(0); setQ(e.target.value); }} />
        <select className={fieldClass} value={active} onChange={(e) => { setPage(0); setActive(e.target.value); }}>
          <option value="">Any status</option>
          <option value="true">Active</option>
          <option value="false">Suspended</option>
        </select>
        <Input placeholder="Plan" value={plan} onChange={(e) => { setPage(0); setPlan(e.target.value); }} />
        <Input type="date" aria-label="Created from" value={createdFrom} onChange={(e) => { setPage(0); setCreatedFrom(e.target.value); }} />
        <Input type="date" aria-label="Created to" value={createdTo} onChange={(e) => { setPage(0); setCreatedTo(e.target.value); }} />
      </div>
      <DelayedSkeletonGate loading={loading} skeleton={<ListSkeleton rows={6} />}>
        <DataTable
          columns={columns}
          rows={rows}
          page={page + 1}
          pageSize={20}
          total={total}
          onPageChange={(next) => setPage(Math.max(0, next - 1))}
          getRowKey={(row) => row.id}
          emptyTitle="No organizations"
          emptyIcon={Icons.organizations}
        />
      </DelayedSkeletonGate>
      <Dialog open={open} title="Create organization" onClose={() => setOpen(false)}>
        <FormField label="Name" htmlFor="org-name"><Input id="org-name" value={name} onChange={(e) => setName(e.target.value)} /></FormField>
        <FormField label="Slug" htmlFor="org-slug"><Input id="org-slug" value={slug} onChange={(e) => setSlug(e.target.value)} /></FormField>
        <FormField label="Description" htmlFor="org-description"><Input id="org-description" value={description} onChange={(e) => setDescription(e.target.value)} /></FormField>
        <FormField label="Owner user id" htmlFor="org-owner"><Input id="org-owner" value={ownerUserId} onChange={(e) => setOwnerUserId(e.target.value)} /></FormField>
        <Button type="button" onClick={() => createOrg().catch((e) => setError(e instanceof ApiError ? e.message : "Create failed"))}>
          Create
        </Button>
      </Dialog>
    </div>
  );
}

function exportQuery(query: Record<string, string | number | boolean | undefined>) {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(query)) {
    if (key === "page" || key === "size" || value === undefined || value === "") continue;
    search.set(key, String(value));
  }
  const text = search.toString();
  return text ? `?${text}` : "";
}
