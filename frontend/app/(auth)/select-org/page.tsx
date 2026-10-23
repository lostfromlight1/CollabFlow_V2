"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Building2 } from "lucide-react";
import { AuthShell } from "@/components/layout/auth-shell";
import { EmptyState } from "@/components/feedback/empty-state";
import { InlineAlert } from "@/components/feedback/inline-alert";
import {
  DelayedSkeletonGate,
  ListSkeleton,
} from "@/components/feedback/skeleton";
import { StaggerItem, StaggerList } from "@/components/motion/stagger-list";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { LogoutButton } from "@/components/auth/logout-button";
import { listMyOrganizations, selectOrganization } from "@/lib/api/auth";
import { ApiError } from "@/lib/api/client";
import { getSession, setSession } from "@/lib/auth/session";
import { postAuthPath } from "@/lib/auth/routing";
import type { OrganizationSummary } from "@/lib/types/api";

export default function SelectOrgPage() {
  const router = useRouter();
  const [orgs, setOrgs] = useState<OrganizationSummary[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const session = getSession();
    if (!session) {
      router.replace("/login");
      return;
    }
    listMyOrganizations()
      .then(setOrgs)
      .catch((e) =>
        setError(e instanceof ApiError ? e.message : "Failed to load orgs")
      )
      .finally(() => setLoading(false));
  }, [router]);

  async function choose(organizationId: string) {
    setError(null);
    try {
      const auth = await selectOrganization(organizationId);
      const session = getSession();
      if (!session) {
        router.replace("/login");
        return;
      }
      setSession({
        ...session,
        accessToken: auth.accessToken,
        refreshToken: auth.refreshToken,
        organizationId: auth.selectedOrganizationId,
        organizations: (auth.organizations || []).map((o) => ({
          id: o.id,
          name: o.name,
          slug: o.slug,
        })),
      });
      router.replace(postAuthPath(session.user, auth));
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Select failed");
    }
  }

  return (
    <AuthShell>
      <div className="mb-3 flex justify-end">
        <LogoutButton />
      </div>
      <Card className="border-border/60 shadow-sm">
        <CardHeader>
          <CardTitle className="font-heading text-xl">Choose organization</CardTitle>
          <CardDescription>
            Sets org context for later X-Organization-Id product calls.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <DelayedSkeletonGate loading={loading} skeleton={<ListSkeleton rows={3} />}>
            <div className="flex flex-col gap-2">
              {error ? <InlineAlert>{error}</InlineAlert> : null}
              {!loading && orgs.length === 0 ? (
                <EmptyState
                  title="No organizations"
                  description="Ask a master admin to attach you to an org."
                  icon={Building2}
                />
              ) : null}
              <StaggerList className="flex flex-col gap-2">
                {orgs.map((o, i) => (
                  <StaggerItem key={o.id} index={i}>
                    <button
                      type="button"
                      onClick={() => choose(o.id)}
                      className="interactive-hover-row interactive-press interactive-focus flex w-full items-center justify-between rounded-lg border border-border/60 px-3 py-3 text-left text-sm"
                    >
                      <span className="font-medium">{o.name}</span>
                      <span className="text-muted-foreground">{o.slug}</span>
                    </button>
                  </StaggerItem>
                ))}
              </StaggerList>
            </div>
          </DelayedSkeletonGate>
        </CardContent>
      </Card>
    </AuthShell>
  );
}
