"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils";

const LINKS = [
  { suffix: "", label: "Overview" },
  { suffix: "/tasks", label: "Tasks" },
  { suffix: "/board", label: "Board" },
  { suffix: "/sprints", label: "Sprints" },
  { suffix: "/documents", label: "Documents" },
  { suffix: "/members", label: "Members" },
  { suffix: "/settings", label: "Settings" },
];

export function ProjectNav({ projectId }: { projectId: string }) {
  const pathname = usePathname();
  const base = `/projects/${projectId}`;
  return (
    <nav className="mb-4 flex flex-wrap gap-1">
      {LINKS.map((link) => {
        const href = `${base}${link.suffix}`;
        const active = link.suffix === "" ? pathname === href : pathname.startsWith(href);
        return (
          <Link
            key={href}
            href={href}
            className={cn(
              "interactive-hover-fill interactive-press rounded-lg px-3 py-1.5 text-sm",
              active ? "bg-primary text-primary-foreground" : "text-muted-foreground"
            )}
          >
            {link.label}
          </Link>
        );
      })}
    </nav>
  );
}