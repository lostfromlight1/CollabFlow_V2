"use client";

import { useCallback, useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { PageHeader } from "@/components/layout/page-header";
import { ProjectNav } from "@/components/product/project-nav";
import { MemberManager } from "@/components/product/member-manager";
import { InlineAlert } from "@/components/feedback/inline-alert";
import {
  addProjectMember,
  listProjectMembers,
  listRoles,
  removeProjectMember,
  updateProjectMemberRole,
  type Member,
  type RoleOption,
} from "@/lib/api/platform";
import { ApiError } from "@/lib/api/client";
import { getSession } from "@/lib/auth/session";
import { canManageProject } from "@/lib/auth/permissions";
import { Icons } from "@/lib/icons";

export default function ProjectMembersPage() {
  const params = useParams<{ id: string }>();
  const [members, setMembers] = useState<Member[]>([]);
  const [roles, setRoles] = useState<RoleOption[]>([]);
  const [canManage, setCanManage] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    const [m, r] = await Promise.all([listProjectMembers(params.id), listRoles("PROJECT")]);
    setMembers(m);
    setRoles(r);
    const me = getSession()?.user.id;
    setCanManage(canManageProject(m.find((row) => row.userId === me)?.roleId));
  }, [params.id]);

  useEffect(() => {
    load().catch((e) => setError(e instanceof ApiError ? e.message : "Load failed"));
  }, [load]);

  return (
    <div className="space-y-4">
      <ProjectNav projectId={params.id} />
      <PageHeader title="Project members" description="Must already belong to the workspace." icon={Icons.users} />
      {error ? <InlineAlert>{error}</InlineAlert> : null}
      <MemberManager
        members={members}
        roles={roles}
        canManage={canManage}
        onAdd={async (input) => {
          await addProjectMember(params.id, input);
          await load();
        }}
        onRole={async (memberId, roleId) => {
          await updateProjectMemberRole(params.id, memberId, roleId);
          await load();
        }}
        onRemove={async (memberId) => {
          await removeProjectMember(params.id, memberId);
          await load();
        }}
      />
    </div>
  );
}
