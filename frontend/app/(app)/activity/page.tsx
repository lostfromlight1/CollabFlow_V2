"use client";

import { useEffect, useState } from "react";
import { PageHeader } from "@/components/layout/page-header";
import { InlineAlert } from "@/components/feedback/inline-alert";
import { DataTable, type DataColumn } from "@/components/data/data-table";
import { listOrgActivity, type ActivityItem } from "@/lib/api/collab";
import { ApiError } from "@/lib/api/client";
import { Icons } from "@/lib/icons";

export default function ActivityPage() {
  const [rows, setRows] = useState<ActivityItem[]>([]);
  const [page, setPage] = useState(0);
  const [total, setTotal] = useState(0);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    listOrgActivity(page, 20)
      .then((result) => {
        setRows(result.content ?? []);
        setTotal(result.totalElements ?? 0);
      })
      .catch((e) => setError(e instanceof ApiError ? e.message : "Load failed"));
  }, [page]);

  const columns: DataColumn<ActivityItem>[] = [
    { id: "when", header: "When", cell: (r) => r.createdAt },
    { id: "actor", header: "Who", cell: (r) => r.actorName || r.actorEmail },
    { id: "action", header: "Action", cell: (r) => r.actionType },
    { id: "entity", header: "Entity", cell: (r) => `${r.entityType}` },
  ];

  return (
    <div className="space-y-4">
      <PageHeader title="Activity" description="What happened in this organization." icon={Icons.activity} />
      {error ? <InlineAlert>{error}</InlineAlert> : null}
      <DataTable
        columns={columns}
        rows={rows}
        page={page}
        pageSize={20}
        total={total}
        onPageChange={setPage}
        getRowKey={(r) => r.id}
        emptyTitle="No activity yet"
        emptyIcon={Icons.activity}
      />
    </div>
  );
}
