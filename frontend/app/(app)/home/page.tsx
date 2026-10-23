"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { ArrowRight, LayoutGrid } from "lucide-react";
import { FadeIn } from "@/components/motion/fade-in";
import { PageHeader } from "@/components/layout/page-header";
import { Icon } from "@/components/ui/icon";
import { buttonVariants } from "@/components/ui/button";
import { orgDisplayName } from "@/lib/auth/require-org";
import { getSession } from "@/lib/auth/session";
import { Icons } from "@/lib/icons";
import { cn } from "@/lib/utils";

export default function HomePage() {
  const [orgId, setOrgId] = useState<string | null>(null);
  const [orgName, setOrgName] = useState("…");
  const [isAdmin, setIsAdmin] = useState(false);

  useEffect(() => {
    const s = getSession();
    setOrgId(s?.organizationId ?? null);
    setOrgName(orgDisplayName(s?.organizationId ?? null));
    setIsAdmin(!!s?.user.masterAdmin);
  }, []);

  return (
    <FadeIn>
      <PageHeader
        title="Home"
        description="Organization → Workspace → Project → Task. Start with workspaces in your selected org."
        icon={Icons.home}
      />
      <div className="space-y-4 rounded-xl border border-border/60 bg-card p-5">
        <div>
          <p className="text-sm text-muted-foreground">Current organization</p>
          <p className="mt-1 font-medium text-foreground">{orgName}</p>
          {orgId ? (
            <p className="mt-0.5 font-mono text-xs text-muted-foreground">{orgId}</p>
          ) : (
            <p className="mt-1 text-sm text-destructive">
              No org selected —{" "}
              <Link href="/select-org" className="interactive-hover-link underline">
                choose one
              </Link>
              .
            </p>
          )}
        </div>
        <div className="flex flex-wrap gap-3 pt-1">
          <Link
            href="/workspaces"
            className={cn(buttonVariants({ size: "default" }), "gap-2")}
          >
            <Icon icon={LayoutGrid} size="md" />
            Workspaces
          </Link>
          {isAdmin ? (
            <Link
              href="/admin"
              className="interactive-hover-link inline-flex items-center gap-1 text-sm font-medium"
            >
              Open admin portal
              <Icon icon={ArrowRight} size="md" />
            </Link>
          ) : null}
        </div>
      </div>
    </FadeIn>
  );
}
