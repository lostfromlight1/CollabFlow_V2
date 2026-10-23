"use client";

import { AppSidebar, type NavItem } from "@/components/layout/app-sidebar";
import { ShellBanners } from "@/components/layout/shell-banners";
import type { ShellUser } from "@/components/layout/user-menu";
import { PageTransition } from "@/components/motion/page-transition";
import { cn } from "@/lib/utils";

export function AppShell({
  items,
  user,
  children,
  className,
}: {
  items: NavItem[];
  user?: ShellUser;
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <div className={cn("flex min-h-full flex-1", className)}>
      <AppSidebar items={items} user={user} />
      <div className="flex min-w-0 flex-1 flex-col">
        <PageTransition className="flex-1 p-6">
          <ShellBanners />
          {children}
        </PageTransition>
      </div>
    </div>
  );
}
