"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { PageHeader } from "@/components/layout/page-header";
import { InlineAlert } from "@/components/feedback/inline-alert";
import { DataTable, type DataColumn } from "@/components/data/data-table";
import { Dialog } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { FormField } from "@/components/forms/form-field";
import {
  activateAdminUser,
  deactivateAdminUser,
  deleteAdminUser,
  getAdminUser,
  listMembershipsForUser,
  resendAdminVerification,
  resetAdminUserPassword,
  restoreAdminUser,
  revokeAdminUserSessions,
  setMasterAdmin,
  startImpersonation,
  type AdminMembership,
  type AdminUser,
} from "@/lib/api/admin";
import { listUserActivity, type ActivityItem } from "@/lib/api/collab";
import { beginImpersonation } from "@/lib/auth/session";
import { ApiError } from "@/lib/api/client";
import { Icons } from "@/lib/icons";

function when(value: string | null | undefined) {
  if (!value) return "—";
  return value.slice(0, 16).replace("T", " ");
}

export default function AdminUserDetailPage() {
  const params = useParams<{ id: string }>();
  const id = params.id;
  const [user, setUser] = useState<AdminUser | null>(null);
  const [memberships, setMemberships] = useState<AdminMembership[]>([]);
  const [activity, setActivity] = useState<ActivityItem[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [passwordOpen, setPasswordOpen] = useState(false);
  const [confirm, setConfirm] = useState<string | null>(null);
  const [temporaryPassword, setTemporaryPassword] = useState("");

  function load() {
    return Promise.all([getAdminUser(id), listMembershipsForUser(id), listUserActivity(id)])
      .then(([profile, orgs, feed]) => {
        setUser(profile);
        setMemberships(orgs ?? []);
        setActivity(feed.content ?? []);
      });
  }

  useEffect(() => {
    load().catch((e) => setError(e instanceof ApiError ? e.message : "Could not load user"));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function run(action: () => Promise<unknown>, message: string) {
    setError(null);
    setNotice(null);
    await action();
    setNotice(message);
    setConfirm(null);
    await load();
  }

  async function impersonate() {
    if (!user) return;
    const result = await startImpersonation(user.id);
    beginImpersonation({
      accessToken: result.accessToken,
      user: { id: result.targetUserId, email: result.targetEmail, name: result.targetName, masterAdmin: false },
    });
    window.location.assign("/home");
  }

  const membershipColumns: DataColumn<AdminMembership>[] = [
    {
      id: "org",
      header: "Organization",
      cell: (row) => (
        <Link href={`/admin/organizations/${row.organizationId}`} className="text-primary">
          {row.name}
        </Link>
      ),
    },
    { id: "slug", header: "Slug", cell: (row) => row.slug },
    { id: "role", header: "Role", cell: (row) => row.roleName },
    { id: "status", header: "Status", cell: (row) => (row.active ? "Active" : "Suspended") },
  ];

  const activityColumns: DataColumn<ActivityItem>[] = [
    { id: "when", header: "When", cell: (row) => when(row.createdAt) },
    { id: "action", header: "Action", cell: (row) => row.actionType },
    { id: "entity", header: "Entity", cell: (row) => `${row.entityType}` },
  ];

  if (!user) {
    return error ? <InlineAlert>{error}</InlineAlert> : <p className="text-sm text-muted-foreground">Loading user…</p>;
  }

  const deleted = Boolean(user.deletedAt);

  return (
    <div className="space-y-6">
      <PageHeader
        title={user.name}
        description={user.email}
        icon={Icons.users}
        badge={
          <Badge variant={deleted ? "warn" : user.active ? "success" : "secondary"}>
            {deleted ? "Deleted" : user.active ? "Active" : "Suspended"}
          </Badge>
        }
        actions={
          <Link href={`/admin/audit?targetType=USER&targetId=${user.id}`} className="text-sm font-medium text-primary">
            Audit
          </Link>
        }
      />
      {error ? <InlineAlert>{error}</InlineAlert> : null}
      {notice ? <InlineAlert variant="success">{notice}</InlineAlert> : null}
      <dl className="grid gap-2 text-sm sm:grid-cols-2">
        <div>Verified: {user.emailVerified ? "Yes" : "No"}</div>
        <div>Master admin: {user.masterAdmin ? "Yes" : "No"}</div>
        <div>Created: {when(user.createdAt)}</div>
        <div>Last login: {when(user.lastLoginAt)}</div>
      </dl>
      <div className="flex flex-wrap gap-2">
        {deleted ? (
          <Button type="button" onClick={() => run(() => restoreAdminUser(id), "User restored")}>Restore</Button>
        ) : (
          <>
            {user.active ? (
              <Button type="button" variant="outline" onClick={() => setConfirm("deactivate")}>Suspend</Button>
            ) : (
              <Button type="button" onClick={() => run(() => activateAdminUser(id), "User activated")}>Activate</Button>
            )}
            <Button type="button" variant="outline" onClick={() => run(() => revokeAdminUserSessions(id), "Sessions revoked")}>
              Revoke sessions
            </Button>
            <Button type="button" variant="outline" onClick={() => setPasswordOpen(true)}>Reset password</Button>
            <Button type="button" variant="outline" onClick={() => run(() => resendAdminVerification(id), "Verification checked")}>
              Resend verification
            </Button>
            <Button type="button" variant="outline" onClick={() => setConfirm("master")}>
              {user.masterAdmin ? "Remove master admin" : "Grant master admin"}
            </Button>
            <Button type="button" variant="outline" onClick={() => impersonate().catch((e) => setError(e instanceof ApiError ? e.message : "Impersonation failed"))}>
              View as user
            </Button>
            <Button type="button" variant="destructive" onClick={() => setConfirm("delete")}>Delete</Button>
          </>
        )}
      </div>
      <section className="space-y-2">
        <h2 className="font-heading text-lg font-semibold">Organizations</h2>
        <DataTable
          columns={membershipColumns}
          rows={memberships}
          page={1}
          pageSize={Math.max(memberships.length, 1)}
          total={memberships.length}
          onPageChange={() => undefined}
          getRowKey={(row) => row.organizationId}
          emptyTitle="No organizations"
          emptyIcon={Icons.organizations}
        />
      </section>
      <section className="space-y-2">
        <h2 className="font-heading text-lg font-semibold">Activity</h2>
        <DataTable
          columns={activityColumns}
          rows={activity}
          page={1}
          pageSize={Math.max(activity.length, 1)}
          total={activity.length}
          onPageChange={() => undefined}
          getRowKey={(row) => row.id}
          emptyTitle="No activity"
          emptyIcon={Icons.activity}
        />
      </section>
      <Dialog open={passwordOpen} title="Temporary password" onClose={() => setPasswordOpen(false)}>
        <FormField label="Temporary password" htmlFor="temp-password">
          <Input id="temp-password" type="password" value={temporaryPassword} onChange={(e) => setTemporaryPassword(e.target.value)} />
        </FormField>
        <Button
          type="button"
          onClick={() =>
            run(() => resetAdminUserPassword(id, temporaryPassword), "Password reset").then(() => {
              setPasswordOpen(false);
              setTemporaryPassword("");
            })
          }
        >
          Save
        </Button>
      </Dialog>
      <Dialog open={confirm !== null} title="Confirm" onClose={() => setConfirm(null)}>
        <p className="text-sm text-muted-foreground">
          {confirm === "delete"
            ? "Soft-delete this user and revoke their sessions."
            : confirm === "deactivate"
              ? "Suspend this user and revoke their sessions."
              : "Change this user's master admin flag."}
        </p>
        <Button
          type="button"
          variant={confirm === "delete" ? "destructive" : "default"}
          onClick={() => {
            if (confirm === "delete") return run(() => deleteAdminUser(id), "User deleted");
            if (confirm === "deactivate") return run(() => deactivateAdminUser(id), "User suspended");
            return run(() => setMasterAdmin(id, !user.masterAdmin), "Master admin updated");
          }}
        >
          Confirm
        </Button>
      </Dialog>
    </div>
  );
}
