"use client";

import { useEffect, useState } from "react";
import { PageHeader } from "@/components/layout/page-header";
import { InlineAlert } from "@/components/feedback/inline-alert";
import { DataTable, type DataColumn } from "@/components/data/data-table";
import { Button } from "@/components/ui/button";
import { acceptInvitation, cancelInvitation, listMyInvitations, type Invitation } from "@/lib/api/collab";
import { ApiError } from "@/lib/api/client";
import { Icons } from "@/lib/icons";

export default function MyInvitationsPage() {
  const [rows, setRows] = useState<Invitation[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);
  const [page, setPage] = useState(0);

  async function load() {
    const result = await listMyInvitations();
    setRows(result.content ?? []);
  }

  useEffect(() => {
    load().catch((e) => setError(e instanceof ApiError ? e.message : "Load failed"));
  }, []);

  const columns: DataColumn<Invitation>[] = [
    { id: "org", header: "Organization", cell: (r) => r.organizationId },
    { id: "status", header: "Status", cell: (r) => r.status },
    {
      id: "actions",
      header: "",
      cell: (r) =>
        r.status === "PENDING" ? (
          <div className="flex gap-2">
            <Button
              type="button"
              size="sm"
              onClick={() => {
                void acceptInvitation(r.token)
                  .then(() => {
                    setInfo("Invitation accepted.");
                    return load();
                  })
                  .catch((e) => setError(e instanceof ApiError ? e.message : "Accept failed"));
              }}
            >
              Accept
            </Button>
            <Button
              type="button"
              size="sm"
              variant="outline"
              onClick={() => {
                void cancelInvitation(r.id)
                  .then(load)
                  .catch((e) => setError(e instanceof ApiError ? e.message : "Decline failed"));
              }}
            >
              Decline
            </Button>
          </div>
        ) : null,
    },
  ];

  return (
    <div className="space-y-4">
      <PageHeader title="My invitations" description="Invites sent to your email." icon={Icons.mail} />
      {error ? <InlineAlert>{error}</InlineAlert> : null}
      {info ? <InlineAlert variant="success">{info}</InlineAlert> : null}
      <DataTable
        columns={columns}
        rows={rows.slice(page * 8, page * 8 + 8)}
        page={page}
        pageSize={8}
        total={rows.length}
        onPageChange={setPage}
        getRowKey={(r) => r.id}
        emptyTitle="No invitations"
        emptyIcon={Icons.mail}
      />
    </div>
  );
}
