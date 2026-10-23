"use client";

import { useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import { PageHeader } from "@/components/layout/page-header";
import { InlineAlert } from "@/components/feedback/inline-alert";
import { DataTable, type DataColumn } from "@/components/data/data-table";
import { Input } from "@/components/ui/input";
import { listAdminAudit, type AuditItem } from "@/lib/api/admin";
import { ApiError } from "@/lib/api/client";
import { Icons } from "@/lib/icons";
import { Suspense } from "react";

function AuditTable() {
  const search = useSearchParams();
  const [rows, setRows] = useState<AuditItem[]>([]);
  const [page, setPage] = useState(0);
  const [total, setTotal] = useState(0);
  const [action, setAction] = useState("");
  const [actorUserId, setActorUserId] = useState("");
  const [targetType, setTargetType] = useState(search.get("targetType") ?? "");
  const [targetId, setTargetId] = useState(search.get("targetId") ?? "");
  const [createdFrom, setCreatedFrom] = useState("");
  const [createdTo, setCreatedTo] = useState("");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    listAdminAudit({
      action: action || undefined,
      actorUserId: actorUserId || undefined,
      targetType: targetType || undefined,
      targetId: targetId || undefined,
      createdFrom: createdFrom ? `${createdFrom}T00:00:00.000Z` : undefined,
      createdTo: createdTo ? `${createdTo}T23:59:59.999Z` : undefined,
      page,
      size: 20,
    })
      .then((result) => {
        setRows(result.content ?? []);
        setTotal(result.totalElements ?? 0);
        setError(null);
      })
      .catch((e) => setError(e instanceof ApiError ? e.message : "Could not load audit"));
  }, [action, actorUserId, targetType, targetId, createdFrom, createdTo, page]);

  const columns: DataColumn<AuditItem>[] = [
    { id: "when", header: "When", cell: (row) => row.createdAt?.slice(0, 16).replace("T", " ") },
    { id: "action", header: "Action", cell: (row) => row.action },
    { id: "actor", header: "Actor", cell: (row) => row.actorUserId },
    { id: "target", header: "Target", cell: (row) => `${row.targetType} ${row.targetId ?? row.metadata ?? ""}` },
    { id: "ip", header: "IP", cell: (row) => row.ipAddress || "—" },
  ];

  return (
    <div className="space-y-4">
      <PageHeader title="Audit" description="Who changed what in the admin console." icon={Icons.audit} />
      {error ? <InlineAlert>{error}</InlineAlert> : null}
      <div className="grid gap-2 sm:grid-cols-3">
        <Input placeholder="Action" value={action} onChange={(e) => { setPage(0); setAction(e.target.value); }} />
        <Input placeholder="Actor user id" value={actorUserId} onChange={(e) => { setPage(0); setActorUserId(e.target.value); }} />
        <Input placeholder="Target type" value={targetType} onChange={(e) => { setPage(0); setTargetType(e.target.value); }} />
        <Input placeholder="Target id" value={targetId} onChange={(e) => { setPage(0); setTargetId(e.target.value); }} />
        <Input type="date" aria-label="From" value={createdFrom} onChange={(e) => { setPage(0); setCreatedFrom(e.target.value); }} />
        <Input type="date" aria-label="To" value={createdTo} onChange={(e) => { setPage(0); setCreatedTo(e.target.value); }} />
      </div>
      <DataTable
        columns={columns}
        rows={rows}
        page={page + 1}
        pageSize={20}
        total={total}
        onPageChange={(next) => setPage(Math.max(0, next - 1))}
        getRowKey={(row) => row.id}
        emptyTitle="No audit events"
        emptyIcon={Icons.audit}
      />
    </div>
  );
}

export default function AdminAuditPage() {
  return (
    <Suspense fallback={<p className="text-sm text-muted-foreground">Loading audit…</p>}>
      <AuditTable />
    </Suspense>
  );
}
