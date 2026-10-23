"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { PageHeader } from "@/components/layout/page-header";
import { ProjectNav } from "@/components/product/project-nav";
import { InlineAlert } from "@/components/feedback/inline-alert";
import { DelayedSkeletonGate, DashboardSkeleton } from "@/components/feedback/skeleton";
import { Badge } from "@/components/ui/badge";
import { StatusChip } from "@/components/ui/status-chip";
import { PriorityChip } from "@/components/ui/priority-chip";
import { listEntityActivity, type ActivityItem } from "@/lib/api/collab";
import { getProject, type Project } from "@/lib/api/platform";
import { getTaskStats, type TaskStats } from "@/lib/api/tasks";
import { ApiError } from "@/lib/api/client";
import { priorityTone, statusTone } from "@/lib/task-meta";
import { Icons } from "@/lib/icons";

export default function ProjectHomePage() {
  const params = useParams<{ id: string }>();
  const [project, setProject] = useState<Project | null>(null);
  const [stats, setStats] = useState<TaskStats | null>(null);
  const [activity, setActivity] = useState<ActivityItem[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      getProject(params.id),
      getTaskStats(params.id).catch(() => null),
      listEntityActivity("PROJECT", params.id).catch(() => ({ content: [] as ActivityItem[] })),
    ])
      .then(([p, s, a]) => {
        setProject(p);
        setStats(s);
        setActivity(a.content ?? []);
      })
      .catch((e) => setError(e instanceof ApiError ? e.message : "Could not load project"))
      .finally(() => setLoading(false));
  }, [params.id]);

  return (
    <DelayedSkeletonGate loading={loading} skeleton={<DashboardSkeleton />}>
      <ProjectNav projectId={params.id} />
      <PageHeader
        title={project?.name || "Project"}
        description={project?.description || "Overview, tasks, and activity."}
        icon={Icons.projects}
        badge={project?.status ? <Badge variant="secondary">{project.status}</Badge> : null}
      />
      {error ? <InlineAlert>{error}</InlineAlert> : null}
      <div className="grid gap-4 md:grid-cols-2">
        <section className="rounded-xl border border-border/60 bg-card p-4">
          <h2 className="mb-3 text-sm font-medium">Tasks</h2>
          <p className="text-2xl font-semibold">{stats?.total ?? 0}</p>
          <ul className="mt-3 space-y-2">
            {(stats?.byStatus ?? []).map((row) => (
              <li key={row.statusId} className="flex items-center justify-between gap-2 text-sm">
                <StatusChip status={statusTone(row.name, row.category)} label={row.name} />
                <span>{row.count}</span>
              </li>
            ))}
          </ul>
          <ul className="mt-3 flex flex-wrap gap-2">
            {(stats?.byPriority ?? []).map((row) => (
              <li key={row.priority} className="flex items-center gap-1 text-xs">
                <PriorityChip priority={priorityTone(row.priority)} />
                <span>{row.count}</span>
              </li>
            ))}
          </ul>
          <Link href={`/projects/${params.id}/tasks`} className="interactive-hover-link mt-3 inline-block text-sm">
            Open task table
          </Link>
        </section>
        <section className="rounded-xl border border-border/60 bg-card p-4">
          <h2 className="mb-3 text-sm font-medium">Recent activity</h2>
          {activity.length === 0 ? (
            <p className="text-sm text-muted-foreground">No activity yet.</p>
          ) : (
            <ul className="space-y-2 text-sm">
              {activity.slice(0, 8).map((item) => (
                <li key={item.id}>
                  <span className="font-medium">{item.actorName}</span> {item.actionType}{" "}
                  <span className="text-muted-foreground">{item.entityType}</span>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>
    </DelayedSkeletonGate>
  );
}
