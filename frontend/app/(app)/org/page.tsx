"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { PageHeader } from "@/components/layout/page-header";
import { InlineAlert } from "@/components/feedback/inline-alert";
import { DelayedSkeletonGate, ListSkeleton } from "@/components/feedback/skeleton";
import { RoleChip, roleTone } from "@/components/ui/role-chip";
import { buttonVariants } from "@/components/ui/button";
import { getOrganization, type Organization } from "@/lib/api/platform";
import { ApiError } from "@/lib/api/client";
import { requireOrgSession } from "@/lib/auth/require-org";
import { canManageOrg, isOrgOwner } from "@/lib/auth/permissions";
import { Icons } from "@/lib/icons";
import { cn } from "@/lib/utils";

export default function OrgPage() {
  const router = useRouter();
  const [org, setOrg] = useState<Organization | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const s = requireOrgSession();
    if (!s?.organizationId) {
      router.replace("/select-org");
      return;
    }
    getOrganization(s.organizationId)
      .then(setOrg)
      .catch((e) => setError(e instanceof ApiError ? e.message : "Could not load organization"))
      .finally(() => setLoading(false));
  }, [router]);

  return (
    <DelayedSkeletonGate loading={loading} skeleton={<ListSkeleton rows={2} />}>
      <PageHeader
        title={org?.name || "Organization"}
        description={org?.description || "People, invites, and settings for this organization."}
        icon={Icons.organizations}
        badge={org?.roleName ? <RoleChip role={roleTone(org.roleName)} label={org.roleName} /> : null}
      />
      {error ? <InlineAlert>{error}</InlineAlert> : null}
      {org ? (
        <div className="flex flex-wrap gap-2">
          <Link href="/org/members" className={cn(buttonVariants({ variant: "outline" }))}>Members</Link>
          {canManageOrg(org.roleId) ? (
            <Link href="/org/invitations" className={cn(buttonVariants({ variant: "outline" }))}>Invitations</Link>
          ) : null}
          <Link href="/org/settings" className={cn(buttonVariants({ variant: "outline" }))}>
            {isOrgOwner(org.roleId) || canManageOrg(org.roleId) ? "Settings" : "Details"}
          </Link>
          <Link href="/org/new" className={cn(buttonVariants({ variant: "ghost" }))}>New organization</Link>
        </div>
      ) : null}
      {org ? (
        <dl className="mt-6 grid max-w-lg gap-2 text-sm">
          <div className="flex justify-between gap-4"><dt className="text-muted-foreground">Slug</dt><dd>{org.slug}</dd></div>
          <div className="flex justify-between gap-4"><dt className="text-muted-foreground">Plan</dt><dd>{org.plan || "—"}</dd></div>
          <div className="flex justify-between gap-4"><dt className="text-muted-foreground">Id</dt><dd className="font-mono text-xs">{org.id}</dd></div>
        </dl>
      ) : null}
    </DelayedSkeletonGate>
  );
}
