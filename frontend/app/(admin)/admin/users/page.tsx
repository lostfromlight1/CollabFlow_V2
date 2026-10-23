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
  adminCreateUser,
  downloadAdminCsv,
  listAdminUsers,
  type AdminUser,
} from "@/lib/api/admin";
import { ApiError } from "@/lib/api/client";
import { Icons } from "@/lib/icons";

const fieldClass = "h-8 w-full rounded-lg border border-input bg-background px-2 text-sm";

function when(value: string | null | undefined) {
  if (!value) return "—";
  return value.slice(0, 16).replace("T", " ");
}

export default function AdminUsersPage() {
  const [rows, setRows] = useState<AdminUser[]>([]);
  const [page, setPage] = useState(0);
  const [total, setTotal] = useState(0);
  const [sort, setSort] = useState("createdAt,desc");
  const [q, setQ] = useState("");
  const [active, setActive] = useState("");
  const [masterAdmin, setMasterAdmin] = useState("");
  const [emailVerified, setEmailVerified] = useState("");
  const [organizationId, setOrganizationId] = useState("");
  const [createdFrom, setCreatedFrom] = useState("");
  const [createdTo, setCreatedTo] = useState("");
  const [deleted, setDeleted] = useState("false");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [temporaryPassword, setTemporaryPassword] = useState("");

  const query = {
    q,
    active: active === "" ? undefined : active === "true",
    masterAdmin: masterAdmin === "" ? undefined : masterAdmin === "true",
    emailVerified: emailVerified === "" ? undefined : emailVerified === "true",
    organizationId: organizationId || undefined,
    createdFrom: createdFrom ? `${createdFrom}T00:00:00.000Z` : undefined,
    createdTo: createdTo ? `${createdTo}T23:59:59.999Z` : undefined,
    deleted,
    page,
    size: 20,
    sort,
  };

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    listAdminUsers(query)
      .then((result) => {
        if (cancelled) return;
        setRows(result.content ?? []);
        setTotal(result.totalElements ?? 0);
        setError(null);
      })
      .catch((e) => {
        if (!cancelled) setError(e instanceof ApiError ? e.message : "Could not load users");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [q, active, masterAdmin, emailVerified, organizationId, createdFrom, createdTo, deleted, page, sort]);

  function toggleSort(field: string) {
    setPage(0);
    setSort((current) => (current === `${field},asc` ? `${field},desc` : `${field},asc`));
  }

  function header(field: string, label: string) {
    return (
      <button type="button" className="font-medium" onClick={() => toggleSort(field)}>
        {label}
      </button>
    );
  }

  const columns: DataColumn<AdminUser>[] = [
    {
      id: "name",
      header: header("name", "Name"),
      cell: (row) => (
        <Link href={`/admin/users/${row.id}`} className="font-medium text-primary">
          {row.name}
        </Link>
      ),
    },
    { id: "email", header: header("email", "Email"), cell: (row) => row.email },
    {
      id: "status",
      header: "Status",
      cell: (row) => (
        <Badge variant={row.deletedAt ? "warn" : row.active ? "success" : "secondary"}>
          {row.deletedAt ? "Deleted" : row.active ? "Active" : "Suspended"}
        </Badge>
      ),
    },
    {
      id: "role",
      header: "Master admin",
      cell: (row) => (row.masterAdmin ? "Yes" : "No"),
    },
    {
      id: "verified",
      header: "Verified",
      cell: (row) => (row.emailVerified ? "Yes" : "No"),
    },
    { id: "created", header: header("createdAt", "Created"), cell: (row) => when(row.createdAt) },
  ];

  async function createUser() {
    setError(null);
    await adminCreateUser({ name, email, temporaryPassword });
    setCreateOpen(false);
    setName("");
    setEmail("");
    setTemporaryPassword("");
    setPage(0);
    setSort((current) => current);
    const result = await listAdminUsers({ ...query, page: 0 });
    setRows(result.content ?? []);
    setTotal(result.totalElements ?? 0);
  }

  return (
    <div className="space-y-4">
      <PageHeader
        title="Users"
        description="Every account in CollabFlow."
        icon={Icons.users}
        actions={
          <div className="flex gap-2">
            <Button
              type="button"
              variant="outline"
              onClick={() =>
                downloadAdminCsv(`/api/v1/admin/users/export${toQuery(query)}`, "users.csv").catch((e) =>
                  setError(e instanceof Error ? e.message : "Export failed")
                )
              }
            >
              Export CSV
            </Button>
            <Button type="button" onClick={() => setCreateOpen(true)}>
              Create user
            </Button>
          </div>
        }
      />
      {error ? <InlineAlert>{error}</InlineAlert> : null}
      <div className="grid gap-2 sm:grid-cols-4">
        <Input placeholder="Search name or email" value={q} onChange={(e) => { setPage(0); setQ(e.target.value); }} />
        <select className={fieldClass} value={active} onChange={(e) => { setPage(0); setActive(e.target.value); }}>
          <option value="">Any status</option>
          <option value="true">Active</option>
          <option value="false">Suspended</option>
        </select>
        <select className={fieldClass} value={masterAdmin} onChange={(e) => { setPage(0); setMasterAdmin(e.target.value); }}>
          <option value="">Any master admin</option>
          <option value="true">Master admin</option>
          <option value="false">Not master admin</option>
        </select>
        <select className={fieldClass} value={emailVerified} onChange={(e) => { setPage(0); setEmailVerified(e.target.value); }}>
          <option value="">Any verification</option>
          <option value="true">Verified</option>
          <option value="false">Unverified</option>
        </select>
        <Input placeholder="Organization id" value={organizationId} onChange={(e) => { setPage(0); setOrganizationId(e.target.value); }} />
        <Input type="date" value={createdFrom} onChange={(e) => { setPage(0); setCreatedFrom(e.target.value); }} />
        <Input type="date" value={createdTo} onChange={(e) => { setPage(0); setCreatedTo(e.target.value); }} />
        <select className={fieldClass} value={deleted} onChange={(e) => { setPage(0); setDeleted(e.target.value); }}>
          <option value="false">Hide deleted</option>
          <option value="true">Deleted only</option>
          <option value="any">Include deleted</option>
        </select>
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
          emptyTitle="No users"
          emptyDescription="Create a user or clear the filters."
          emptyIcon={Icons.users}
        />
      </DelayedSkeletonGate>
      <Dialog open={createOpen} title="Create user" onClose={() => setCreateOpen(false)}>
        <FormField label="Name" htmlFor="create-name">
          <Input id="create-name" value={name} onChange={(e) => setName(e.target.value)} />
        </FormField>
        <FormField label="Email" htmlFor="create-email">
          <Input id="create-email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
        </FormField>
        <FormField label="Temporary password" htmlFor="create-password">
          <Input id="create-password" type="password" value={temporaryPassword} onChange={(e) => setTemporaryPassword(e.target.value)} />
        </FormField>
        <Button
          type="button"
          onClick={() => createUser().catch((e) => setError(e instanceof ApiError ? e.message : "Create failed"))}
        >
          Create
        </Button>
      </Dialog>
    </div>
  );
}

function toQuery(query: Record<string, string | number | boolean | undefined>) {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(query)) {
    if (key === "page" || key === "size" || value === undefined || value === "") continue;
    search.set(key, String(value));
  }
  const text = search.toString();
  return text ? `?${text}` : "";
}
