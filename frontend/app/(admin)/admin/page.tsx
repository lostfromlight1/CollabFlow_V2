"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { PageHeader } from "@/components/layout/page-header";
import { InlineAlert } from "@/components/feedback/inline-alert";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { getAdminStats, type AdminStats } from "@/lib/api/admin";
import { ApiError } from "@/lib/api/client";
import { Icons } from "@/lib/icons";

const tiles: { key: keyof AdminStats; label: string }[] = [
  { key: "usersTotal", label: "Users" },
  { key: "usersActive", label: "Active users" },
  { key: "usersInactive", label: "Suspended users" },
  { key: "usersDeleted", label: "Deleted users" },
  { key: "activeUsersLast7Days", label: "Logged in, 7 days" },
  { key: "signupsLast7Days", label: "Signups, 7 days" },
  { key: "signupsLast30Days", label: "Signups, 30 days" },
  { key: "orgsTotal", label: "Organizations" },
  { key: "orgsActive", label: "Active organizations" },
  { key: "orgsInactive", label: "Suspended organizations" },
];

export default function AdminHomePage() {
  const [stats, setStats] = useState<AdminStats | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getAdminStats()
      .then(setStats)
      .catch((e) => setError(e instanceof ApiError ? e.message : "Could not load stats"));
  }, []);

  return (
    <div className="space-y-6">
      <PageHeader
        title="Master admin"
        description="Users, organizations, and what changed."
        icon={Icons.admin}
      />
      {error ? <InlineAlert>{error}</InlineAlert> : null}
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        {tiles.map((tile) => (
          <Card key={tile.key}>
            <CardHeader>
              <CardTitle>{tile.label}</CardTitle>
            </CardHeader>
            <CardContent className="text-2xl font-semibold">
              {stats ? String(stats[tile.key] ?? "—") : "—"}
            </CardContent>
          </Card>
        ))}
      </div>
      <section className="space-y-2">
        <h2 className="font-heading text-lg font-semibold">Signups, last 30 days</h2>
        <ul className="space-y-1 text-sm">
          {(stats?.signupsByDay ?? []).length === 0 ? <li className="text-muted-foreground">No signups in this window.</li> : null}
          {(stats?.signupsByDay ?? []).map((day) => (
            <li key={day.date} className="flex justify-between gap-4 border-b border-border/50 py-1">
              <span>{day.date}</span>
              <span>{day.count}</span>
            </li>
          ))}
        </ul>
      </section>
      <section className="space-y-2">
        <div className="flex items-center justify-between">
          <h2 className="font-heading text-lg font-semibold">Recent audit</h2>
          <Link href="/admin/audit" className="text-sm text-primary">Open audit</Link>
        </div>
        <ul className="space-y-1 text-sm">
          {(stats?.recentAudit ?? []).length === 0 ? <li className="text-muted-foreground">No admin actions yet.</li> : null}
          {(stats?.recentAudit ?? []).map((row) => (
            <li key={row.id}>
              {row.createdAt?.slice(0, 16).replace("T", " ")} · {row.action} · {row.targetType}
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}
