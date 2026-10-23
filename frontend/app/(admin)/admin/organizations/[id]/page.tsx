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
  activateAdminOrganization,
  adminAttachMember,
  adminTransferOwnership,
  deactivateAdminOrganization,
  getAdminOrganization,
  listAdminOrgMembers,
  ORG_MEMBER_ROLE_ID,
  type AdminMember,
  type AdminOrganizationDetail,
} from "@/lib/api/admin";
import { ApiError } from "@/lib/api/client";
import { Icons } from "@/lib/icons";

export default function AdminOrganizationDetailPage() {
  const params = useParams<{ id: string }>();
  const id = params.id;
  const [detail, setDetail] = useState<AdminOrganizationDetail | null>(null);
  const [members, setMembers] = useState<AdminMember[]>([]);
  const [memberTotal, setMemberTotal] = useState(0);
  const [memberPage, setMemberPage] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [attachOpen, setAttachOpen] = useState(false);
  const [transferOpen, setTransferOpen] = useState(false);
  const [userId, setUserId] = useState("");
  const [roleId, setRoleId] = useState(ORG_MEMBER_ROLE_ID);
  const [newOwner, setNewOwner] = useState("");

  function load(page = memberPage) {
    return Promise.all([getAdminOrganization(id), listAdminOrgMembers(id, page, 20)]).then(([org, pageResult]) => {
      setDetail(org);
      setMembers(pageResult.content ?? []);
      setMemberTotal(pageResult.totalElements ?? 0);
    });
  }

  useEffect(() => {
    load(memberPage).catch((e) => setError(e instanceof ApiError ? e.message : "Could not load organization"));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id, memberPage]);

  async function run(action: () => Promise<unknown>, message: string) {
    setError(null);
    await action();
    setNotice(message);
    setAttachOpen(false);
    setTransferOpen(false);
    await load();
  }

  const org = detail?.organization;
  const columns: DataColumn<AdminMember>[] = [
    { id: "name", header: "Name", cell: (row) => row.name || row.userId },
    { id: "email", header: "Email", cell: (row) => row.email || "—" },
    { id: "role", header: "Role", cell: (row) => row.roleName },
  ];

  if (!org || !detail) {
    return error ? <InlineAlert>{error}</InlineAlert> : <p className="text-sm text-muted-foreground">Loading organization…</p>;
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title={org.name}
        description={org.slug}
        icon={Icons.organizations}
        badge={<Badge variant={org.active ? "success" : "secondary"}>{org.active ? "Active" : "Suspended"}</Badge>}
        actions={
          <Link href={`/admin/audit?targetType=ORGANIZATION&targetId=${org.id}`} className="text-sm font-medium text-primary">
            Audit
          </Link>
        }
      />
      {error ? <InlineAlert>{error}</InlineAlert> : null}
      {notice ? <InlineAlert variant="success">{notice}</InlineAlert> : null}
      <dl className="grid gap-2 text-sm sm:grid-cols-2">
        <div>Owner: {org.ownerName || org.ownerEmail || org.ownerId}</div>
        <div>Plan: {org.plan}</div>
        <div>Created: {org.createdAt?.slice(0, 16).replace("T", " ")}</div>
        <div>Members: {detail.memberCount}</div>
        <div>Workspaces: {detail.workspaceCount}</div>
        <div>Projects: {detail.projectCount}</div>
      </dl>
      <div className="flex flex-wrap gap-2">
        {org.active ? (
          <Button type="button" variant="outline" onClick={() => run(() => deactivateAdminOrganization(id), "Organization suspended")}>
            Suspend
          </Button>
        ) : (
          <Button type="button" onClick={() => run(() => activateAdminOrganization(id), "Organization activated")}>
            Activate
          </Button>
        )}
        <Button type="button" variant="outline" onClick={() => setAttachOpen(true)}>Attach member</Button>
        <Button type="button" variant="outline" onClick={() => setTransferOpen(true)}>Transfer ownership</Button>
      </div>
      <section className="space-y-2">
        <h2 className="font-heading text-lg font-semibold">Workspaces</h2>
        <ul className="space-y-1 text-sm">
          {detail.workspaces.length === 0 ? <li className="text-muted-foreground">No workspaces</li> : null}
          {detail.workspaces.map((workspace) => (
            <li key={workspace.id}>
              {workspace.name} · {workspace.projectCount} projects · {workspace.active ? "active" : "inactive"}
            </li>
          ))}
        </ul>
      </section>
      <section className="space-y-2">
        <h2 className="font-heading text-lg font-semibold">Members</h2>
        <DataTable
          columns={columns}
          rows={members}
          page={memberPage + 1}
          pageSize={20}
          total={memberTotal}
          onPageChange={(next) => setMemberPage(Math.max(0, next - 1))}
          getRowKey={(row) => row.id}
          emptyTitle="No members"
          emptyIcon={Icons.users}
        />
      </section>
      <Dialog open={attachOpen} title="Attach member" onClose={() => setAttachOpen(false)}>
        <FormField label="User id" htmlFor="attach-user"><Input id="attach-user" value={userId} onChange={(e) => setUserId(e.target.value)} /></FormField>
        <FormField label="Role id" htmlFor="attach-role"><Input id="attach-role" value={roleId} onChange={(e) => setRoleId(e.target.value)} /></FormField>
        <Button type="button" onClick={() => run(() => adminAttachMember(id, { userId, roleId }), "Member attached")}>Attach</Button>
      </Dialog>
      <Dialog open={transferOpen} title="Transfer ownership" onClose={() => setTransferOpen(false)}>
        <p className="text-sm text-muted-foreground">The new owner must already be a member.</p>
        <FormField label="New owner user id" htmlFor="new-owner"><Input id="new-owner" value={newOwner} onChange={(e) => setNewOwner(e.target.value)} /></FormField>
        <Button type="button" onClick={() => run(() => adminTransferOwnership(id, newOwner), "Ownership transferred")}>Transfer</Button>
      </Dialog>
    </div>
  );
}
