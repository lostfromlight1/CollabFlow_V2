"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { PageHeader } from "@/components/layout/page-header";
import { MemberManager } from "@/components/product/member-manager";
import { InlineAlert } from "@/components/feedback/inline-alert";
import {
  addOrgMember,
  getOrganization,
  listOrgMembers,
  listRoles,
  removeOrgMember,
  updateOrgMemberRole,
  type Member,
  type Organization,
  type RoleOption,
} from "@/lib/api/platform";
import { ApiError } from "@/lib/api/client";
import { requireOrgSession } from "@/lib/auth/require-org";
import { ROLES, canManageOrg } from "@/lib/auth/permissions";
import { Icons } from "@/lib/icons";

export default function OrgMembersPage() {
  const router = useRouter();
  const [org, setOrg] = useState<Organization | null>(null);
  const [members, setMembers] = useState<Member[]>([]);
  const [roles, setRoles] = useState<RoleOption[]>([]);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async (orgId: string) => {
    const [o, m, r] = await Promise.all([
      getOrganization(orgId),
      listOrgMembers(orgId),
      listRoles("ORGANIZATION"),
    ]);
    setOrg(o);
    setMembers(m);
    setRoles(r);
  }, []);

  useEffect(() => {
    const s = requireOrgSession();
    if (!s?.organizationId) {
      router.replace("/select-org");
      return;
    }
    load(s.organizationId).catch((e) => setError(e instanceof ApiError ? e.message : "Load failed"));
  }, [load, router]);

  if (!org) return error ? <InlineAlert>{error}</InlineAlert> : null;

  return (
    <div>
      <PageHeader title="Members" description="Organization members and roles." icon={Icons.users} />
      {error ? <InlineAlert>{error}</InlineAlert> : null}
      <MemberManager
        members={members}
        roles={roles}
        canManage={canManageOrg(org.roleId)}
        ownerRoleId={ROLES.ORG_OWNER}
        onAdd={async (input) => {
          await addOrgMember(org.id, input);
          await load(org.id);
        }}
        onRole={async (memberId, roleId) => {
          await updateOrgMemberRole(org.id, memberId, roleId);
          await load(org.id);
        }}
        onRemove={async (memberId) => {
          await removeOrgMember(org.id, memberId);
          await load(org.id);
        }}
      />
    </div>
  );
}
