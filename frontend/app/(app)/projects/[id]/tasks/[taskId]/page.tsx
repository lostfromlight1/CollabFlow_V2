"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { PageHeader } from "@/components/layout/page-header";
import { ProjectNav } from "@/components/product/project-nav";
import { CommentThread } from "@/components/product/comment-thread";
import { FormField } from "@/components/forms/form-field";
import { InlineAlert } from "@/components/feedback/inline-alert";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { PriorityChip } from "@/components/ui/priority-chip";
import { listEntityActivity, type ActivityItem } from "@/lib/api/collab";
import { listProjectMembers, type Member } from "@/lib/api/platform";
import {
  assignTask,
  createSubtask,
  getDefaultWorkflow,
  getTask,
  listSubtasks,
  updateTask,
  type Task,
  type WorkflowStatus,
} from "@/lib/api/tasks";
import { ApiError } from "@/lib/api/client";
import { getSession } from "@/lib/auth/session";
import { canWriteTasks } from "@/lib/auth/permissions";
import { priorityTone, statusTone } from "@/lib/task-meta";
import { StatusChip } from "@/components/ui/status-chip";
import { useProjectRoom } from "@/lib/realtime/use-project-room";
import { Icons } from "@/lib/icons";

const schema = z.object({
  name: z.string().min(1).max(255),
  description: z.string().max(10000).optional(),
  priority: z.enum(["LOW", "MEDIUM", "HIGH", "CRITICAL"]),
  subtask: z.string().max(255).optional(),
});

type TaskForm = z.infer<typeof schema>;

export default function TaskDetailPage() {
  const params = useParams<{ id: string; taskId: string }>();
  const [task, setTask] = useState<Task | null>(null);
  const [subs, setSubs] = useState<Task[]>([]);
  const [statuses, setStatuses] = useState<WorkflowStatus[]>([]);
  const [members, setMembers] = useState<Member[]>([]);
  const [activity, setActivity] = useState<ActivityItem[]>([]);
  const [canWrite, setCanWrite] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [tick, setTick] = useState(0);
  const reload = useCallback(() => setTick((n) => n + 1), []);
  useProjectRoom(params.id, reload);

  const { register, handleSubmit, reset, getValues, formState } = useForm<TaskForm>({
    resolver: zodResolver(schema),
    defaultValues: { name: "", description: "", priority: "MEDIUM" as const, subtask: "" },
  });

  useEffect(() => {
    const me = getSession()?.user.id;
    Promise.all([
      getTask(params.taskId),
      listSubtasks(params.taskId),
      getDefaultWorkflow(),
      listProjectMembers(params.id),
      listEntityActivity("TASK", params.taskId).catch(() => ({ content: [] as ActivityItem[] })),
    ])
      .then(([t, children, wf, people, feed]) => {
        setTask(t);
        setSubs(children);
        setStatuses(wf.statuses);
        setMembers(people);
        setActivity(feed.content ?? []);
        const role = people.find((m) => m.userId === me)?.roleId;
        setCanWrite(canWriteTasks(role));
        const priority = t.priority === "LOW" || t.priority === "HIGH" || t.priority === "CRITICAL" ? t.priority : "MEDIUM";
        reset({
          name: t.name,
          description: t.description ?? "",
          priority,
          subtask: "",
        });
      })
      .catch((e) => setError(e instanceof ApiError ? e.message : "Load failed"));
  }, [params.id, params.taskId, reset, tick]);

  const status = statuses.find((s) => s.id === task?.statusId);

  const onSave = handleSubmit(async (values) => {
    setError(null);
    try {
      setTask(await updateTask(params.taskId, {
        name: values.name,
        description: values.description,
        priority: values.priority,
      }));
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Save failed");
    }
  });

  return (
    <div className="space-y-6">
      <ProjectNav projectId={params.id} />
      <PageHeader
        title={task?.name || "Task"}
        description="Edit details, assign, add subtasks, and comment."
        icon={Icons.projects}
        badge={
          <span className="flex gap-2">
            {status ? <StatusChip status={statusTone(status.name, status.category)} label={status.name} /> : null}
            <PriorityChip priority={priorityTone(task?.priority)} />
          </span>
        }
      />
      <Link href={`/projects/${params.id}/tasks`} className="interactive-hover-link text-sm text-muted-foreground">Back to tasks</Link>
      {error ? <InlineAlert>{error}</InlineAlert> : null}
      <form className="max-w-xl space-y-3" onSubmit={onSave}>
        <FormField label="Name" htmlFor="name" error={formState.errors.name?.message}>
          <Input id="name" disabled={!canWrite} {...register("name")} />
        </FormField>
        <FormField label="Description" htmlFor="description">
          <Input id="description" disabled={!canWrite} {...register("description")} />
        </FormField>
        <FormField label="Priority" htmlFor="priority">
          <select id="priority" disabled={!canWrite} className="h-8 w-full rounded-lg border border-input bg-background px-2 text-sm" {...register("priority")}>
            <option value="LOW">Low</option>
            <option value="MEDIUM">Medium</option>
            <option value="HIGH">High</option>
            <option value="CRITICAL">Urgent</option>
          </select>
        </FormField>
        <FormField label="Assignee" htmlFor="assignee">
          <select
            id="assignee"
            disabled={!canWrite}
            className="h-8 w-full rounded-lg border border-input bg-background px-2 text-sm"
            value={task?.assigneeId ?? ""}
            onChange={(e) => {
              const value = e.target.value || null;
              void assignTask(params.taskId, value)
                .then(setTask)
                .catch((err) => setError(err instanceof ApiError ? err.message : "Assign failed"));
            }}
          >
            <option value="">Unassigned</option>
            {members.map((m) => (
              <option key={m.userId} value={m.userId}>{m.name || m.email || m.userId}</option>
            ))}
          </select>
        </FormField>
        {canWrite ? <Button type="submit" disabled={formState.isSubmitting}>Save</Button> : null}
      </form>
      <section className="space-y-2">
        <h2 className="text-sm font-medium">Subtasks</h2>
        <ul className="space-y-1 text-sm">
          {subs.map((s) => (
            <li key={s.id}>
              <Link href={`/projects/${params.id}/tasks/${s.id}`} className="interactive-hover-link">{s.name}</Link>
            </li>
          ))}
        </ul>
        {canWrite ? (
          <div className="flex gap-2">
            <Input placeholder="Subtask name" {...register("subtask")} />
            <Button
              type="button"
              variant="outline"
              onClick={() => {
                const name = getValues("subtask")?.trim();
                if (!name) return;
                void createSubtask(params.taskId, { name })
                  .then(() => listSubtasks(params.taskId))
                  .then(setSubs)
                  .catch((e) => setError(e instanceof ApiError ? e.message : "Subtask failed"));
              }}
            >
              Add
            </Button>
          </div>
        ) : null}
      </section>
      <section>
        <h2 className="mb-2 text-sm font-medium">Activity</h2>
        <ul className="space-y-1 text-sm text-muted-foreground">
          {activity.map((item) => (
            <li key={item.id}>{item.actorName} · {item.actionType}</li>
          ))}
        </ul>
      </section>
      <CommentThread entityType="TASK" entityId={params.taskId} canWrite={canWrite} />
    </div>
  );
}
