"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { PageHeader } from "@/components/layout/page-header";
import { ProjectNav } from "@/components/product/project-nav";
import { InlineAlert } from "@/components/feedback/inline-alert";
import { DataTable, type DataColumn } from "@/components/data/data-table";
import { Button } from "@/components/ui/button";
import { listProjectMembers } from "@/lib/api/platform";
import {
  downloadProjectDocument,
  listProjectDocuments,
  uploadProjectDocument,
  type ProjectDocument,
} from "@/lib/api/documents";
import { ApiError } from "@/lib/api/client";
import { getSession } from "@/lib/auth/session";
import { canWriteTasks } from "@/lib/auth/permissions";
import { Icons } from "@/lib/icons";

export default function DocumentsPage() {
  const params = useParams<{ id: string }>();
  const [rows, setRows] = useState<ProjectDocument[]>([]);
  const [canWrite, setCanWrite] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);

  async function load() {
    const data = await listProjectDocuments(params.id);
    setRows(data.items ?? []);
  }

  useEffect(() => {
    const me = getSession()?.user.id;
    listProjectMembers(params.id)
      .then((members) => setCanWrite(canWriteTasks(members.find((m) => m.userId === me)?.roleId)))
      .catch(() => setCanWrite(false));
    load().catch((e) => setError(e instanceof ApiError ? e.message : "Load failed"));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [params.id]);

  const columns: DataColumn<ProjectDocument>[] = [
    { id: "name", header: "Name", cell: (r) => r.name },
    { id: "type", header: "Type", cell: (r) => r.fileType },
    { id: "size", header: "Size", cell: (r) => `${r.fileSize} B` },
    { id: "version", header: "Version", cell: (r) => r.version },
    {
      id: "open",
      header: "",
      cell: (r) => (
        <Button
          type="button"
          size="sm"
          variant="outline"
          onClick={() => {
            void downloadProjectDocument(r.id, r.name).catch((e) =>
              setError(e instanceof ApiError ? e.message : "Download failed")
            );
          }}
        >
          Download
        </Button>
      ),
    },
  ];

  return (
    <div className="space-y-4">
      <ProjectNav projectId={params.id} />
      <PageHeader title="Documents" description="Files stored for this project." icon={Icons.projects} />
      {error ? <InlineAlert>{error}</InlineAlert> : null}
      {canWrite ? (
        <input
          type="file"
          className="text-sm"
          onChange={(e) => {
            const file = e.target.files?.[0];
            if (!file) return;
            void uploadProjectDocument(params.id, file)
              .then(load)
              .catch((err) => setError(err instanceof ApiError ? err.message : "Upload failed"));
          }}
        />
      ) : (
        <p className="text-sm text-muted-foreground">Viewers can download files. Uploading needs project membership.</p>
      )}
      <DataTable
        columns={columns}
        rows={rows.slice(page * 8, page * 8 + 8)}
        page={page}
        pageSize={8}
        total={rows.length}
        onPageChange={setPage}
        getRowKey={(r) => r.id}
        emptyTitle="No documents"
        emptyIcon={Icons.projects}
      />
    </div>
  );
}
