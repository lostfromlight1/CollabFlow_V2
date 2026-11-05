"use client";

import { useEffect, useState } from "react";
import { PageHeader } from "@/components/layout/page-header";
import { InlineAlert } from "@/components/feedback/inline-alert";
import { Dialog } from "@/components/ui/dialog";
import { ConfirmDialog } from "@/components/feedback/confirm-dialog";
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
import { errorText } from "@/lib/api/client";
import { Icons } from "@/lib/icons";

const empty = { title: "", body: "", active: true, startsAt: "", endsAt: "" };

function when(value: string | null) {
  if (!value) return "Open";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "Open";
  return date.toLocaleString(undefined, {
    month: "short",
    day: "numeric",
    hour: "numeric",
    minute: "2-digit",
  });
}

function phase(row: Announcement): "Showing" | "Scheduled" | "Ended" | "Off" {
  const now = Date.now();
  const start = row.startsAt ? new Date(row.startsAt).getTime() : null;
  const end = row.endsAt ? new Date(row.endsAt).getTime() : null;
  if (end !== null && end <= now) return "Ended";
  if (!row.active) return "Off";
  if (start !== null && start > now) return "Scheduled";
  return "Showing";
}

export default function AdminAnnouncementsPage() {
  const [rows, setRows] = useState<Announcement[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<string | null>(null);
  const [form, setForm] = useState(empty);
  const [pending, setPending] = useState<{ kind: "stop" | "remove"; row: Announcement } | null>(null);

  function load() {
    return listAnnouncements().then((list) => setRows(list ?? []));
  }

  useEffect(() => {
    load().catch((e) => setError(errorText(e, "Could not load announcements")));
  }, []);

  function openCreate() {
    setEditing(null);
    setForm(empty);
    setError(null);
    setOpen(true);
  }

  function openEdit(row: Announcement) {
    setEditing(row.id);
    setForm({
      title: row.title,
      body: row.body,
      active: row.active,
      startsAt: row.startsAt ? toLocalInput(row.startsAt) : "",
      endsAt: row.endsAt ? toLocalInput(row.endsAt) : "",
    });
    setError(null);
    setOpen(true);
  }

  async function save() {
    const title = form.title.trim();
    const body = form.body.trim();
    if (!title || !body) {
      setError("Title and message are required");
      return;
    }
    const startsAt = form.startsAt ? new Date(form.startsAt).toISOString() : null;
    const endsAt = form.endsAt ? new Date(form.endsAt).toISOString() : null;
    if (startsAt && endsAt && new Date(endsAt).getTime() <= new Date(startsAt).getTime()) {
      setError("End time must be after the start time");
      return;
    }
    const payload = { title, body, active: form.active, startsAt, endsAt };
    if (editing) await updateAnnouncement(editing, payload);
    else await createAnnouncement(payload);
    setOpen(false);
    await load();
  }

  async function remove(row: Announcement) {
    setError(null);
    await deleteAnnouncement(row.id);
    await load();
  }

  async function stopShowing(row: Announcement) {
    setError(null);
    await updateAnnouncement(row.id, {
      title: row.title,
      body: row.body,
      active: false,
      startsAt: row.startsAt,
      endsAt: row.endsAt,
    });
    await load();
  }

  return (
    <div className="space-y-4">
      <PageHeader
        title="Announcements"
        description="One banner on the signed-in shell. It shows only inside the start and end window."
        icon={Icons.announcements}
        actions={<Button type="button" onClick={openCreate}>New announcement</Button>}
      />
      {error && !open ? <InlineAlert>{error}</InlineAlert> : null}
      {rows.length === 0 ? (
        <p className="rounded-xl border border-dashed border-border px-4 py-8 text-center text-sm text-muted-foreground">
          No announcements. The shell stays clear until you publish one.
        </p>
      ) : (
        <ul className="space-y-3">
          {rows.map((row) => {
            const status = phase(row);
            return (
              <li key={row.id} className="rounded-xl border border-border/70 bg-card p-4">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div className="min-w-0 space-y-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <h2 className="font-heading text-base font-medium">{row.title}</h2>
                      <Badge variant={status === "Showing" ? "success" : status === "Scheduled" ? "info" : "secondary"}>
                        {status}
                      </Badge>
                    </div>
                    <p className="whitespace-pre-wrap text-sm text-muted-foreground">{row.body}</p>
                    <p className="text-xs text-muted-foreground">
                      {when(row.startsAt)} → {when(row.endsAt)}
                    </p>
                  </div>
                  <div className="flex shrink-0 gap-2">
                    <Button type="button" size="sm" variant="outline" onClick={() => openEdit(row)}>Edit</Button>
                    {row.active && status !== "Ended" ? (
                      <Button
                        type="button"
                        size="sm"
                        variant="destructive"
                        onClick={() => setPending({ kind: "stop", row })}
                      >
                        Stop showing
                      </Button>
                    ) : null}
                    <Button
                      type="button"
                      size="sm"
                      variant="destructive"
                      onClick={() => setPending({ kind: "remove", row })}
                    >
                      Remove
                    </Button>
                  </div>
                </div>
              </li>
            );
          })}
        </ul>
      )}
      <Dialog open={open} title={editing ? "Edit announcement" : "New announcement"} onClose={() => setOpen(false)}>
        {error && open ? <InlineAlert>{error}</InlineAlert> : null}
        <FormField label="Title" htmlFor="ann-title">
          <Input id="ann-title" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} />
        </FormField>
        <FormField label="Message" htmlFor="ann-body">
          <textarea
            id="ann-body"
            rows={4}
            value={form.body}
            onChange={(e) => setForm({ ...form, body: e.target.value })}
            className="w-full rounded-lg border border-input bg-card/60 px-2.5 py-2 text-sm outline-none"
          />
        </FormField>
        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })} />
          Show on the banner
        </label>
        <div className="grid gap-3 sm:grid-cols-2">
          <FormField label="Starts" htmlFor="ann-start">
            <Input id="ann-start" type="datetime-local" value={form.startsAt} onChange={(e) => setForm({ ...form, startsAt: e.target.value })} />
          </FormField>
          <FormField label="Ends" htmlFor="ann-end">
            <Input id="ann-end" type="datetime-local" value={form.endsAt} onChange={(e) => setForm({ ...form, endsAt: e.target.value })} />
          </FormField>
        </div>
        <p className="text-xs text-muted-foreground">
          Leave a time empty for no limit. After the end time, the banner turns off on its own.
        </p>
        <Button type="button" onClick={() => save().catch((e) => setError(errorText(e, "Save failed")))}>Save</Button>
      </Dialog>
      <ConfirmDialog
        open={pending !== null}
        title={pending?.kind === "remove" ? "Remove announcement" : "Stop showing"}
        message={
          pending?.kind === "remove"
            ? "This deletes the announcement for everyone."
            : "The banner turns off for everyone until you turn it on again."
        }
        confirmLabel={pending?.kind === "remove" ? "Remove" : "Stop showing"}
        onClose={() => setPending(null)}
        onConfirm={() => {
          const action = pending;
          setPending(null);
          if (!action) return;
          const job = action.kind === "remove" ? remove(action.row) : stopShowing(action.row);
          void job.catch((e) => setError(errorText(e, action.kind === "remove" ? "Remove failed" : "Could not stop the banner")));
        }}
      />
    </div>
  );
}

function toLocalInput(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}
