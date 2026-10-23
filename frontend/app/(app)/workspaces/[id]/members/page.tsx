"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { PageHeader } from "@/components/layout/page-header";
import { MemberManager } from "@/components/product/member-manager";
import { InlineAlert } from "@/components/feedback/inline-alert";
import {
  addWorkspaceMember,
  listRoles,
  listWorkspaceMembers,
  removeWorkspaceMember,
  updateWorkspaceMemberRole,
  type Member,
  type RoleOption,
} from "@/lib/api/platform";
import { ApiError } from "@/lib/api/client";
import { getSession } from "@/lib/auth/session";
import { canManageWorkspace } from "@/lib/auth/permissions";
import { Icons } from "@/lib/icons";

export default function WorkspaceMembersPage() {
  const params = useParams<{ id: string }>();
  const [members, setMembers] = useState<Member[]>([]);
  const [roles, setRoles] = useState<RoleOption[]>([]);
  const [canManage, setCanManage] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    const [m, r] = await Promise.all([listWorkspaceMembers(params.id), listRoles("WORKSPACE")]);
    setMembers(m);
    setRoles(r);
    const me = getSession()?.user.id;
    setCanManage(canManageWorkspace(m.find((row) => row.userId === me)?.roleId));
  }, [params.id]);

  useEffect(() => {
    load().catch((e) => setError(e instanceof ApiError ? e.message : "Load failed"));
  }, [load]);

  return (
    <div className="space-y-4">
      <PageHeader title="Workspace members" description="People in this workspace." icon={Icons.users} />
      <Link href={`/workspaces/${params.id}`} className="interactive-hover-link text-sm text-muted-foreground">Back to workspace</Link>
      {error ? <InlineAlert>{error}</InlineAlert> : null}
      <MemberManager
        members={members}
        roles={roles}
        canManage={canManage}
        onAdd={async (input) => {
          await addWorkspaceMember(params.id, input);
          await load();
        }}
        onRole={async (memberId, roleId) => {
          await updateWorkspaceMemberRole(params.id, memberId, roleId);
          await load();
        }}
        onRemove={async (memberId) => {
          await removeWorkspaceMember(params.id, memberId);
          await load();
        }}
      />
    </div>
  );
}
