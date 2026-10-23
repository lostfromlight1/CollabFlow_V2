"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { PageHeader } from "@/components/layout/page-header";
import { FormField } from "@/components/forms/form-field";
import { InlineAlert } from "@/components/feedback/inline-alert";
import { DataTable, type DataColumn } from "@/components/data/data-table";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  changePassword,
  deactivateAccount,
  listSessions,
  logout,
  revokeAllSessions,
  type AuthSessionRow,
} from "@/lib/api/auth";
import { ApiError } from "@/lib/api/client";
import { clearSession, getSession } from "@/lib/auth/session";
import { Icons } from "@/lib/icons";

const schema = z.object({
  currentPassword: z.string().min(1, "Current password is required"),
  newPassword: z.string().min(8, "At least 8 characters").max(100),
});

export default function SecurityPage() {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);
  const [sessions, setSessions] = useState<AuthSessionRow[]>([]);
  const [page, setPage] = useState(0);
  const { register, handleSubmit, reset, formState } = useForm({
    resolver: zodResolver(schema),
    defaultValues: { currentPassword: "", newPassword: "" },
  });

  async function load() {
    try {
      setSessions(await listSessions());
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Could not load sessions");
    }
  }

  useEffect(() => {
    void load();
  }, []);

  const onPassword = handleSubmit(async (values) => {
    setError(null);
    setInfo(null);
    try {
      await changePassword(values.currentPassword, values.newPassword);
      reset();
      setInfo("Password changed.");
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Could not change password");
    }
  });

  const columns: DataColumn<AuthSessionRow>[] = [
    { id: "id", header: "Session", cell: (r) => <span className="font-mono text-xs">{r.id}</span> },
    { id: "created", header: "Created", cell: (r) => r.createdAt },
    { id: "expires", header: "Expires", cell: (r) => r.expiresAt },
  ];

  return (
    <div className="space-y-8">
      <PageHeader title="Security" description="Password, sessions, and account status." icon={Icons.settings} />
      <form className="max-w-md space-y-3" onSubmit={onPassword}>
        <FormField label="Current password" htmlFor="currentPassword" error={formState.errors.currentPassword?.message}>
          <Input id="currentPassword" type="password" {...register("currentPassword")} />
        </FormField>
        <FormField label="New password" htmlFor="newPassword" error={formState.errors.newPassword?.message}>
          <Input id="newPassword" type="password" {...register("newPassword")} />
        </FormField>
        <Button type="submit" disabled={formState.isSubmitting}>Change password</Button>
      </form>
      {info ? <InlineAlert variant="success">{info}</InlineAlert> : null}
      {error ? <InlineAlert>{error}</InlineAlert> : null}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <h2 className="text-sm font-medium">Active sessions</h2>
          <Button
            type="button"
            variant="outline"
            onClick={() => {
              void revokeAllSessions()
                .then(async () => {
                  const s = getSession();
                  if (s?.refreshToken) await logout(s.refreshToken);
                  clearSession();
                  router.replace("/login");
                })
                .catch((e) => setError(e instanceof ApiError ? e.message : "Revoke failed"));
            }}
          >
            Revoke all and sign out
          </Button>
        </div>
        <DataTable
          columns={columns}
          rows={sessions.slice(page * 8, page * 8 + 8)}
          page={page}
          pageSize={8}
          total={sessions.length}
          onPageChange={setPage}
          getRowKey={(r) => r.id}
          emptyTitle="No sessions"
          emptyIcon={Icons.settings}
        />
      </div>
      <Button
        type="button"
        variant="destructive"
        onClick={() => {
          if (!window.confirm("Deactivate this account?")) return;
          void deactivateAccount()
            .then(() => {
              clearSession();
              router.replace("/");
            })
            .catch((e) => setError(e instanceof ApiError ? e.message : "Deactivate failed"));
        }}
      >
        Deactivate account
      </Button>
    </div>
  );
}
