"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { PageHeader } from "@/components/layout/page-header";
import { ProjectNav } from "@/components/product/project-nav";
import { TaskPreview } from "@/components/product/task-preview";
import { InlineAlert } from "@/components/feedback/inline-alert";
import { PriorityChip } from "@/components/ui/priority-chip";
import { StatusChip } from "@/components/ui/status-chip";
import { SoftScroll } from "@/components/ui/soft-scroll";
import { listProjectMembers } from "@/lib/api/platform";
import {
  changeTaskStatus,
  getDefaultWorkflow,
  listTasks,
  type Task,
  type WorkflowStatus,
} from "@/lib/api/tasks";
import { ApiError } from "@/lib/api/client";
import { getSession } from "@/lib/auth/session";
import { canWriteTasks } from "@/lib/auth/permissions";
import { priorityTone, statusTone } from "@/lib/task-meta";
import { useProjectRoom } from "@/lib/realtime/use-project-room";
import { Icons } from "@/lib/icons";

export default function BoardPage() {
  const params = useParams<{ id: string }>();
  const [statuses, setStatuses] = useState<WorkflowStatus[]>([]);
  const [tasks, setTasks] = useState<Task[]>([]);
  const [canMove, setCanMove] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [tick, setTick] = useState(0);

  const reload = useCallback(() => setTick((n) => n + 1), []);
  useProjectRoom(params.id, reload);

  useEffect(() => {
    const me = getSession()?.user.id;
    Promise.all([
      getDefaultWorkflow(),
      listTasks({ projectId: params.id, page: 0, size: 100 }),
      listProjectMembers(params.id),
    ])
      .then(([wf, page, members]) => {
        setStatuses([...wf.statuses].sort((a, b) => a.displayOrder - b.displayOrder));
        setTasks(page.content);
        setCanMove(canWriteTasks(members.find((m) => m.userId === me)?.roleId));
      })
      .catch((e) => setError(e instanceof ApiError ? e.message : "Load failed"));
  }, [params.id, tick]);

  return (
    <div className="space-y-4">
      <ProjectNav projectId={params.id} />
      <PageHeader title="Board" description="Columns follow the default workflow. Move a card to change status." icon={Icons.workspaces} />
      {error ? <InlineAlert>{error}</InlineAlert> : null}
      {!canMove ? <p className="text-sm text-muted-foreground">You can view the board. Moving tasks needs project membership.</p> : null}
      <SoftScroll orientation="horizontal">
        <div className="flex min-w-max gap-3 pb-2">
          {statuses.map((status) => {
            const column = tasks.filter((t) => t.statusId === status.id);
            return (
              <section key={status.id} className="w-72 shrink-0 rounded-xl border border-border/60 bg-muted/30 p-3">
                <div className="mb-3 flex items-center justify-between">
                  <StatusChip status={statusTone(status.name, status.category)} label={status.name} />
                  <span className="text-xs text-muted-foreground">{column.length}</span>
                </div>
                <div className="space-y-2">
                  {column.map((task) => (
                    <TaskPreview key={task.id} task={task} status={status}>
                      <article className="interactive-hover-lift rounded-lg border border-border/60 bg-card p-3">
                        <Link href={`/projects/${params.id}/tasks/${task.id}`} className="interactive-hover-link font-medium">
                          {task.name}
                        </Link>
                        <div className="mt-2">
                          <PriorityChip priority={priorityTone(task.priority)} />
                        </div>
                        {canMove ? (
                          <select
                            className="mt-2 h-8 w-full rounded-lg border border-input bg-background px-2 text-xs"
                            value={task.statusId ?? ""}
                            aria-label={`Move ${task.name}`}
                            onChange={(e) => {
                              void changeTaskStatus(task.id, e.target.value)
                                .then(reload)
                                .catch((err) => setError(err instanceof ApiError ? err.message : "Move failed"));
                            }}
                          >
                            {statuses.map((s) => (
                              <option key={s.id} value={s.id}>{s.name}</option>
                            ))}
                          </select>
                        ) : null}
                      </article>
                    </TaskPreview>
                  ))}
                </div>
              </section>
            );
          })}
        </div>
      </SoftScroll>
    </div>
  );
}
