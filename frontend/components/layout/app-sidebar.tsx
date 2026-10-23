"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import type { LucideIcon } from "lucide-react";
import {
  UserMenu,
  type ShellUser,
} from "@/components/layout/user-menu";
import { Icon } from "@/components/ui/icon";
import { SoftScroll } from "@/components/ui/soft-scroll";
import { cn } from "@/lib/utils";

export type NavItem = {
  href: string;
  label: string;
  icon?: LucideIcon;
};

function isActive(pathname: string, href: string) {
  if (pathname === href) return true;
  if (href === "/admin" || href === "/home") {
    return pathname === href;
  }
  return pathname.startsWith(href);
}

function NavLink({
  href,
  label,
  icon,
  active,
}: {
  href: string;
  label: string;
  icon?: LucideIcon;
  active: boolean;
}) {
  return (
    <Link
      href={href}
      className={cn(
        "interactive-hover-fill interactive-press interactive-focus flex items-center gap-2 rounded-lg px-3 py-2 text-sm font-medium",
        active
          ? "bg-sidebar-primary text-sidebar-primary-foreground"
          : "text-sidebar-foreground/85 hover:bg-sidebar-accent/80"
      )}
    >
      {icon ? <Icon icon={icon} size="md" /> : null}
      <span>{label}</span>
    </Link>
  );
}

export function AppSidebar({
  brand = "CollabFlow",
  items,
  user,
}: {
  brand?: string;
  items: NavItem[];
  user?: ShellUser;
}) {
  const pathname = usePathname();

  return (
    <aside className="flex w-56 shrink-0 flex-col border-r border-sidebar-border bg-sidebar text-sidebar-foreground">
      <div className="border-b border-sidebar-border px-4 py-4">
        <Link
          href="/"
          className="interactive-hover-link font-heading text-lg font-semibold tracking-tight text-primary"
        >
          {brand}
        </Link>
      </div>
      <SoftScroll
        orientation="vertical"
        className="min-h-0 flex-1"
        contentClassName="p-3"
      >
        <nav className="flex flex-col gap-1">
          {items.map((item) => (
            <NavLink
              key={item.href}
              href={item.href}
              label={item.label}
              icon={item.icon}
              active={isActive(pathname, item.href)}
            />
          ))}
        </nav>
      </SoftScroll>
      <div className="mt-auto border-t border-sidebar-border p-2">
        {user ? <UserMenu user={user} /> : null}
      </div>
    </aside>
  );
}
