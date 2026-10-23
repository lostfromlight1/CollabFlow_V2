"use client";

import { useEffect, useState } from "react";
import { PageHeader } from "@/components/layout/page-header";
import { InlineAlert } from "@/components/feedback/inline-alert";
import { DataTable, type DataColumn } from "@/components/data/data-table";
import { Dialog } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { FormField } from "@/components/forms/form-field";
import {
  createAnnouncement,
  deleteAnnouncement,
  listAnnouncements,
  updateAnnouncement,
  type Announcement,
} from "@/lib/api/admin";
import { ApiError } from "@/lib/api/client";
import { Icons } from "@/lib/icons";

const empty = { title: "", body: "", active: true, startsAt: "", endsAt: "" };

export default function AdminAnnouncementsPage() {
  const [rows, setRows] = useState<Announcement[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<string | null>(null);
  const [form, setForm] = useState(empty);

  function load() {
    return listAnnouncements().then((list) => setRows(list ?? []));
  }

  useEffect(() => {
    load().catch((e) => setError(e instanceof ApiError ? e.message : "Could not load announcements"));
  }, []);

  function openCreate() {
    setEditing(null);
    setForm(empty);
    setOpen(true);
  }

  function openEdit(row: Announcement) {
    setEditing(row.id);
    setForm({
      title: row.title,
      body: row.body,
      active: row.active,
      startsAt: row.startsAt ? row.startsAt.slice(0, 16) : "",
      endsAt: row.endsAt ? row.endsAt.slice(0, 16) : "",
    });
    setOpen(true);
  }

  async function save() {
    const body = {
      title: form.title,
      body: form.body,
      active: form.active,
      startsAt: form.startsAt ? new Date(form.startsAt).toISOString() : null,
      endsAt: form.endsAt ? new Date(form.endsAt).toISOString() : null,
    };
    if (editing) await updateAnnouncement(editing, body);
    else await createAnnouncement(body);
    setOpen(false);
    await load();
  }

  const columns: DataColumn<Announcement>[] = [
    { id: "title", header: "Title", cell: (row) => row.title },
    {
      id: "status",
      header: "Status",
      cell: (row) => <Badge variant={row.active ? "success" : "secondary"}>{row.active ? "Active" : "Off"}</Badge>,
    },
    { id: "window", header: "Window", cell: (row) => `${row.startsAt?.slice(0, 10) ?? "now"} → ${row.endsAt?.slice(0, 10) ?? "open"}` },
    {
      id: "actions",
      header: "",
      cell: (row) => (
        <div className="flex gap-2">
          <Button type="button" size="sm" variant="outline" onClick={() => openEdit(row)}>Edit</Button>
          <Button
            type="button"
            size="sm"
            variant="destructive"
            onClick={() => deleteAnnouncement(row.id).then(load).catch((e) => setError(e instanceof ApiError ? e.message : "Delete failed"))}
          >
            Delete
          </Button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-4">
      <PageHeader
        title="Announcements"
        description="One banner on the signed-in shell."
        icon={Icons.announcements}
        actions={<Button type="button" onClick={openCreate}>New announcement</Button>}
      />
      {error ? <InlineAlert>{error}</InlineAlert> : null}
      <DataTable
        columns={columns}
        rows={rows}
        page={1}
        pageSize={Math.max(rows.length, 1)}
        total={rows.length}
        onPageChange={() => undefined}
        getRowKey={(row) => row.id}
        emptyTitle="No announcements"
        emptyIcon={Icons.announcements}
      />
      <Dialog open={open} title={editing ? "Edit announcement" : "New announcement"} onClose={() => setOpen(false)}>
        <FormField label="Title" htmlFor="ann-title"><Input id="ann-title" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} /></FormField>
        <FormField label="Body" htmlFor="ann-body"><Input id="ann-body" value={form.body} onChange={(e) => setForm({ ...form, body: e.target.value })} /></FormField>
        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })} />
          Active
        </label>
        <FormField label="Starts" htmlFor="ann-start"><Input id="ann-start" type="datetime-local" value={form.startsAt} onChange={(e) => setForm({ ...form, startsAt: e.target.value })} /></FormField>
        <FormField label="Ends" htmlFor="ann-end"><Input id="ann-end" type="datetime-local" value={form.endsAt} onChange={(e) => setForm({ ...form, endsAt: e.target.value })} /></FormField>
        <Button type="button" onClick={() => save().catch((e) => setError(e instanceof ApiError ? e.message : "Save failed"))}>Save</Button>
      </Dialog>
    </div>
  );
}
