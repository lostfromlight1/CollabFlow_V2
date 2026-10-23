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
  deleteFeatureFlag,
  listAdminFlags,
  saveFeatureFlag,
  type FeatureFlag,
} from "@/lib/api/admin";
import { clearFeatureFlagCache, useFeatureFlag } from "@/lib/flags";
import { ApiError } from "@/lib/api/client";
import { Icons } from "@/lib/icons";

export default function AdminFeatureFlagsPage() {
  const [rows, setRows] = useState<FeatureFlag[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [open, setOpen] = useState(false);
  const [keyName, setKeyName] = useState("");
  const [description, setDescription] = useState("");
  const [enabled, setEnabled] = useState(false);
  const preview = useFeatureFlag(keyName || "documents");

  function load() {
    clearFeatureFlagCache();
    return listAdminFlags().then((list) => setRows(list ?? []));
  }

  useEffect(() => {
    load().catch((e) => setError(e instanceof ApiError ? e.message : "Could not load flags"));
  }, []);

  async function save() {
    await saveFeatureFlag(keyName, { enabled, description });
    setOpen(false);
    setKeyName("");
    setDescription("");
    setEnabled(false);
    await load();
  }

  const columns: DataColumn<FeatureFlag>[] = [
    { id: "key", header: "Key", cell: (row) => row.key },
    {
      id: "enabled",
      header: "Enabled",
      cell: (row) => <Badge variant={row.enabled ? "success" : "secondary"}>{row.enabled ? "On" : "Off"}</Badge>,
    },
    { id: "description", header: "Description", cell: (row) => row.description || "—" },
    {
      id: "actions",
      header: "",
      cell: (row) => (
        <Button
          type="button"
          size="sm"
          variant="destructive"
          onClick={() => deleteFeatureFlag(row.key).then(load).catch((e) => setError(e instanceof ApiError ? e.message : "Delete failed"))}
        >
          Delete
        </Button>
      ),
    },
  ];

  return (
    <div className="space-y-4">
      <PageHeader
        title="Feature flags"
        description={`A missing key stays enabled. Preview for “${keyName || "documents"}” is ${preview ? "on" : "off"}.`}
        icon={Icons.flags}
        actions={<Button type="button" onClick={() => setOpen(true)}>New flag</Button>}
      />
      {error ? <InlineAlert>{error}</InlineAlert> : null}
      <DataTable
        columns={columns}
        rows={rows}
        page={1}
        pageSize={Math.max(rows.length, 1)}
        total={rows.length}
        onPageChange={() => undefined}
        getRowKey={(row) => row.key}
        emptyTitle="No flags"
        emptyDescription="Existing screens stay on until you add a flag and turn it off."
        emptyIcon={Icons.flags}
      />
      <Dialog open={open} title="Feature flag" onClose={() => setOpen(false)}>
        <FormField label="Key" htmlFor="flag-key">
          <Input id="flag-key" value={keyName} onChange={(e) => setKeyName(e.target.value)} placeholder="documents" />
        </FormField>
        <FormField label="Description" htmlFor="flag-desc">
          <Input id="flag-desc" value={description} onChange={(e) => setDescription(e.target.value)} />
        </FormField>
        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" checked={enabled} onChange={(e) => setEnabled(e.target.checked)} />
          Enabled
        </label>
        <Button type="button" onClick={() => save().catch((e) => setError(e instanceof ApiError ? e.message : "Save failed"))}>Save</Button>
      </Dialog>
    </div>
  );
}
