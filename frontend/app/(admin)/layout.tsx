"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AppShell } from "@/components/layout/app-shell";
import type { ShellUser } from "@/components/layout/user-menu";
import {
  DelayedSkeletonGate,
  ShellSkeleton,
} from "@/components/feedback/skeleton";
import { getSession } from "@/lib/auth/session";
import { Icons } from "@/lib/icons";

const ADMIN_NAV = [
  { href: "/admin", label: "Dashboard", icon: Icons.admin },
  { href: "/admin/users", label: "Users", icon: Icons.users },
  { href: "/admin/organizations", label: "Organizations", icon: Icons.organizations },
  { href: "/admin/audit", label: "Audit", icon: Icons.audit },
  { href: "/admin/announcements", label: "Announcements", icon: Icons.announcements },
  { href: "/admin/feature-flags", label: "Feature flags", icon: Icons.flags },
];

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const [ready, setReady] = useState(false);
  const [user, setUser] = useState<ShellUser | null>(null);

  useEffect(() => {
    const s = getSession();
    if (!s) {
      router.replace("/login");
      return;
    }
    if (!s.user.masterAdmin) {
      router.replace("/home");
      return;
    }
    setUser({ name: s.user.name, email: s.user.email });
    setReady(true);
  }, [router]);

  return (
    <DelayedSkeletonGate loading={!ready} skeleton={<ShellSkeleton />}>
      {ready && user ? (
        <AppShell items={ADMIN_NAV} user={user}>
          {children}
        </AppShell>
      ) : null}
    </DelayedSkeletonGate>
  );
}
