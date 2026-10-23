"use client";

import { useEffect, useState } from "react";
import { PageHeader } from "@/components/layout/page-header";
import { InlineAlert } from "@/components/feedback/inline-alert";
import { DataTable, type DataColumn } from "@/components/data/data-table";
import { Button } from "@/components/ui/button";
import {
  listNotifications,
  markAllNotificationsRead,
  markNotificationRead,
  type NotificationItem,
} from "@/lib/api/collab";
import { ApiError } from "@/lib/api/client";
import { Icons } from "@/lib/icons";

export default function NotificationsPage() {
  const [rows, setRows] = useState<NotificationItem[]>([]);
  const [page, setPage] = useState(0);
  const [total, setTotal] = useState(0);
  const [error, setError] = useState<string | null>(null);

  function load(nextPage = page) {
    return listNotifications(nextPage, 20).then((result) => {
      setRows(result.content ?? []);
      setTotal(result.totalElements ?? 0);
    });
  }

  useEffect(() => {
    load(page).catch((e) => setError(e instanceof ApiError ? e.message : "Load failed"));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page]);

  const columns: DataColumn<NotificationItem>[] = [
    { id: "title", header: "Title", cell: (r) => <span className={r.isRead ? "text-muted-foreground" : "font-medium"}>{r.title}</span> },
    { id: "message", header: "Message", cell: (r) => r.message },
    { id: "when", header: "When", cell: (r) => r.createdAt },
    {
      id: "read",
      header: "",
      cell: (r) =>
        r.isRead ? (
          "Read"
        ) : (
          <Button
            type="button"
            size="sm"
            variant="outline"
            onClick={() => {
              void markNotificationRead(r.id)
                .then(() => load())
                .catch((e) => setError(e instanceof ApiError ? e.message : "Update failed"));
            }}
          >
            Mark read
          </Button>
        ),
    },
  ];

  return (
    <div className="space-y-4">
      <PageHeader
        title="Notifications"
        description="Inbox for this organization."
        icon={Icons.notifications}
        actions={
          <Button
            type="button"
            variant="outline"
            onClick={() => {
              void markAllNotificationsRead()
                .then(() => load())
                .catch((e) => setError(e instanceof ApiError ? e.message : "Update failed"));
            }}
          >
            Mark all read
          </Button>
        }
      />
      {error ? <InlineAlert>{error}</InlineAlert> : null}
      <DataTable
        columns={columns}
        rows={rows}
        page={page}
        pageSize={20}
        total={total}
        onPageChange={setPage}
        getRowKey={(r) => r.id}
        emptyTitle="No notifications"
        emptyIcon={Icons.notifications}
      />
    </div>
  );
}
