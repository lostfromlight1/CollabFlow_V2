"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AppShell } from "@/components/layout/app-shell";
import type { ShellUser } from "@/components/layout/user-menu";
import {
  DelayedSkeletonGate,
  ShellSkeleton,
} from "@/components/feedback/skeleton";
import { unreadNotificationCount } from "@/lib/api/collab";
import { getSession, setSession } from "@/lib/auth/session";
import { Icons } from "@/lib/icons";

const MEMBER_NAV = [
  { href: "/home", label: "Home", icon: Icons.home },
  { href: "/workspaces", label: "Workspaces", icon: Icons.workspaces },
  { href: "/org", label: "Organization", icon: Icons.organizations },
  { href: "/activity", label: "Activity", icon: Icons.activity },
  { href: "/invitations", label: "Invitations", icon: Icons.mail },
];

export default function AppLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const [ready, setReady] = useState(false);
  const [user, setUser] = useState<ShellUser | null>(null);
  const [unread, setUnread] = useState(0);

  useEffect(() => {
    let s = getSession();
    if (!s) {
      router.replace("/login");
      return;
    }
    // Single-org users: auto-bind org context for X-Organization-Id
    if (!s.organizationId && s.organizations?.length === 1) {
      s = { ...s, organizationId: s.organizations[0]!.id };
      setSession(s);
    }
    if (!s.organizationId) {
      router.replace("/select-org");
      return;
    }
    setUser({ name: s.user.name, email: s.user.email });
    setReady(true);
    unreadNotificationCount()
      .then((r) => setUnread(r.count))
      .catch(() => setUnread(0));
  }, [router]);

  const nav = [
    ...MEMBER_NAV,
    {
      href: "/notifications",
      label: unread > 0 ? `Notifications (${unread})` : "Notifications",
      icon: Icons.notifications,
    },
  ];

  return (
    <DelayedSkeletonGate loading={!ready} skeleton={<ShellSkeleton />}>
      {ready && user ? (
        <AppShell items={nav} user={user}>
          {children}
        </AppShell>
      ) : null}
    </DelayedSkeletonGate>
  );
}
