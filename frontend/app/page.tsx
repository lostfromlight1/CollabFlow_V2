"use client";

import { useEffect, useState } from "react";
import { LandingHero } from "@/components/landing/landing-hero";
import { getSession } from "@/lib/auth/session";

type Cta = { href: string; label: string };

export default function RootPage() {
  const [cta, setCta] = useState<Cta | null>(null);

  useEffect(() => {
    const s = getSession();
    if (!s) {
      setCta({ href: "/login", label: "Sign in" });
      return;
    }
    if (s.user.masterAdmin) {
      setCta({ href: "/admin", label: "Open admin" });
      return;
    }
    if (!s.organizationId && (s.organizations?.length ?? 0) > 1) {
      setCta({ href: "/select-org", label: "Choose organization" });
      return;
    }
    setCta({ href: "/home", label: "Go to home" });
  }, []);

  if (!cta) {
    return (
      <main className="flex min-h-[100dvh] flex-1 items-center justify-center bg-background p-6 text-sm text-muted-foreground">
        Loading…
      </main>
    );
  }

  return (
    <main className="flex-1">
      <LandingHero primaryHref={cta.href} primaryLabel={cta.label} />
    </main>
  );
}
