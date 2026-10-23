"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { motion, useReducedMotion } from "framer-motion";
import {
  Building2,
  ChevronUp,
  LogOut,
  Moon,
  Settings,
  Sun,
  UserRound,
} from "lucide-react";
import { Icon } from "@/components/ui/icon";
import { logout } from "@/lib/api/auth";
import { clearSession, getSession } from "@/lib/auth/session";
import { popoverEnterVariants, reducedVariant } from "@/lib/motion/variants";
import { cn } from "@/lib/utils";

export type ShellUser = {
  name: string;
  email: string;
};

function initials(name: string) {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return "?";
  if (parts.length === 1) return parts[0]!.slice(0, 2).toUpperCase();
  return `${parts[0]![0] ?? ""}${parts[1]![0] ?? ""}`.toUpperCase();
}

export function UserMenu({ user }: { user: ShellUser }) {
  const router = useRouter();
  const reduced = useReducedMotion();
  const [open, setOpen] = useState(false);
  const [dark, setDark] = useState(false);
  const rootRef = useRef<HTMLDivElement>(null);
  useEffect(() => {
    setDark(document.documentElement.classList.contains("dark"));
  }, []);

  useEffect(() => {
    if (!open) return;
    function onDoc(e: MouseEvent) {
      if (!rootRef.current?.contains(e.target as Node)) setOpen(false);
    }
    function onKey(e: KeyboardEvent) {
      if (e.key === "Escape") setOpen(false);
    }
    document.addEventListener("mousedown", onDoc);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", onDoc);
      document.removeEventListener("keydown", onKey);
    };
  }, [open]);

  async function onLogout() {
    const s = getSession();
    if (s?.refreshToken) {
      await logout(s.refreshToken);
    }
    clearSession();
    router.replace("/");
  }

  function toggleTheme() {
    const next = !document.documentElement.classList.contains("dark");
    document.documentElement.classList.toggle("dark", next);
    setDark(next);
  }

  const variants = reduced ? reducedVariant : popoverEnterVariants;

  const items: {
    href?: string;
    label: string;
    icon: typeof UserRound;
    onClick?: () => void;
    danger?: boolean;
  }[] = [
    {
      href: "/settings/profile",
      label: "Profile",
      icon: UserRound,
    },
    {
      href: "/settings/security",
      label: "Settings",
      icon: Settings,
    },
    {
      href: "/select-org",
      label: "Switch organization",
      icon: Building2,
    },
    {
      label: dark ? "Theme: Light" : "Theme: Dark",
      icon: dark ? Sun : Moon,
      onClick: toggleTheme,
    },
    {
      label: "Log out",
      icon: LogOut,
      onClick: onLogout,
      danger: true,
    },
  ];

  return (
    <div ref={rootRef} className="relative">
      <button
        type="button"
        aria-expanded={open}
        aria-haspopup="menu"
        onClick={() => setOpen((v) => !v)}
        className={cn(
          "interactive-hover-fill interactive-press interactive-focus flex w-full items-center gap-2 rounded-lg px-2 py-2 text-left"
        )}
      >
        <span className="flex size-9 shrink-0 items-center justify-center rounded-full bg-primary/15 text-xs font-semibold text-primary">
          {initials(user.name)}
        </span>
        <span className="min-w-0 flex-1">
          <span className="block truncate text-sm font-medium text-sidebar-foreground">
            {user.name}
          </span>
          <span className="block truncate text-xs text-muted-foreground">
            {user.email}
          </span>
        </span>
        <Icon
          icon={ChevronUp}
          size="md"
          className={cn(
            "text-muted-foreground",
            !open && "rotate-180"
          )}
          style={{
            transition: `transform var(--motion-fast) var(--motion-ease)`,
          }}
        />
      </button>

      {open ? (
        <motion.div
          role="menu"
          initial="initial"
          animate="animate"
          variants={variants}
          className="absolute bottom-full left-0 z-50 mb-2 w-full min-w-[12rem] overflow-hidden rounded-xl border border-border/70 bg-popover p-1 shadow-lg"
        >
          {items.map((item) => {
            const className = cn(
              "interactive-hover-fill interactive-press interactive-focus flex w-full items-center gap-2 rounded-lg px-2.5 py-2 text-sm",
              item.danger
                ? "text-destructive hover:bg-destructive/10"
                : "text-popover-foreground"
            );
            if (item.onClick && !item.href) {
              return (
                <button
                  key={item.label}
                  type="button"
                  role="menuitem"
                  className={className}
                  onClick={() => {
                    item.onClick?.();
                    if (item.danger) return;
                    setOpen(false);
                  }}
                >
                  <Icon icon={item.icon} size="md" />
                  {item.label}
                </button>
              );
            }
            return (
              <Link
                key={item.label}
                href={item.href || "#"}
                role="menuitem"
                className={className}
                onClick={() => setOpen(false)}
              >
                <Icon icon={item.icon} size="md" />
                {item.label}
              </Link>
            );
          })}
        </motion.div>
      ) : null}
    </div>
  );
}
