"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { PageHeader } from "@/components/layout/page-header";
import { ProjectNav } from "@/components/product/project-nav";
import { CommentThread } from "@/components/product/comment-thread";
import { TaskFormDialog, type TaskFormValues } from "@/components/product/task-form-dialog";
import { FormField } from "@/components/forms/form-field";
import { InlineAlert } from "@/components/feedback/inline-alert";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { PriorityChip } from "@/components/ui/priority-chip";
import { listEntityActivity, type ActivityItem } from "@/lib/api/collab";
import { activitySentence } from "@/lib/activity-label";
import { listProjectMembers, type Member } from "@/lib/api/platform";
import {
  assignTask,
  createSubtask,
  getDefaultWorkflow,
  getTask,
  listSubtasks,
  dueDateFromInput,
  dueDateToInput,
  updateTask,
  type Task,
  type WorkflowStatus,
} from "@/lib/api/tasks";
import { errorText } from "@/lib/api/client";
import { getSession } from "@/lib/auth/session";
import { canWriteTasks } from "@/lib/auth/permissions";
import { priorityTone, statusTone } from "@/lib/task-meta";
import { StatusChip } from "@/components/ui/status-chip";
import { useProjectRoom } from "@/lib/realtime/use-project-room";
import { Icons } from "@/lib/icons";

function formPriority(value: string | null | undefined): TaskFormValues["priority"] {
  if (value === "LOW" || value === "HIGH" || value === "CRITICAL") return value;
  return "MEDIUM";
}

export default function TaskDetailPage() {
  const params = useParams<{ id: string; taskId: string }>();
  const [task, setTask] = useState<Task | null>(null);
  const [subs, setSubs] = useState<Task[]>([]);
  const [statuses, setStatuses] = useState<WorkflowStatus[]>([]);
  const [members, setMembers] = useState<Member[]>([]);
  const [activity, setActivity] = useState<ActivityItem[]>([]);
  const [canWrite, setCanWrite] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [editOpen, setEditOpen] = useState(false);
  const [subtask, setSubtask] = useState("");
  const [tick, setTick] = useState(0);
  const reload = useCallback(() => setTick((n) => n + 1), []);
  useProjectRoom(params.id, reload);

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
      })
      .catch((e) => setError(errorText(e, "Load failed")));
  }, [params.id, params.taskId, tick]);

  const status = statuses.find((s) => s.id === task?.statusId);

  const onSave = async (values: TaskFormValues) => {
    setFormError(null);
    try {
      setTask(await updateTask(params.taskId, {
        name: values.name,
        description: values.description,
        priority: values.priority,
        dueDate: dueDateFromInput(values.dueDate ?? "") ?? null,
      }));
      setEditOpen(false);
    } catch (e) {
      setFormError(errorText(e, "Save failed"));
    }
  };

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
        actions={canWrite ? <Button type="button" onClick={() => { setFormError(null); setEditOpen(true); }}>Edit</Button> : null}
      />
      <Link href={`/projects/${params.id}/tasks`} className="interactive-hover-link text-sm text-muted-foreground">Back to tasks</Link>
      {error ? <InlineAlert>{error}</InlineAlert> : null}
      <dl className="max-w-xl space-y-2 text-sm">
        <div>
          <dt className="text-muted-foreground">Description</dt>
          <dd>{task?.description || "—"}</dd>
        </div>
        <div>
          <dt className="text-muted-foreground">Due</dt>
          <dd>{task?.dueDate ? dueDateToInput(task.dueDate) : "—"}</dd>
        </div>
        <div>
          <dt className="text-muted-foreground">Completed</dt>
          <dd>{task?.closedAt ? new Date(task.closedAt).toLocaleString(undefined, { month: "short", day: "numeric", year: "numeric", hour: "numeric", minute: "2-digit" }) : "Not yet"}</dd>
        </div>
      </dl>
      <div className="max-w-xl">
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
                .catch((err) => setError(errorText(err, "Assign failed")));
            }}
          >
            <option value="">Unassigned</option>
            {members.map((m) => (
              <option key={m.userId} value={m.userId}>{m.name || m.email || m.userId}</option>
            ))}
          </select>
        </FormField>
      </div>
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
            <Input placeholder="Subtask name" value={subtask} onChange={(e) => setSubtask(e.target.value)} />
            <Button
              type="button"
              variant="outline"
              onClick={() => {
                const name = subtask.trim();
                if (!name) return;
                void createSubtask(params.taskId, { name })
                  .then(() => listSubtasks(params.taskId))
                  .then((rows) => {
                    setSubs(rows);
                    setSubtask("");
                  })
                  .catch((e) => setError(errorText(e, "Subtask failed")));
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
            <li key={item.id}>{item.actorName} · {activitySentence(item)}</li>
          ))}
        </ul>
      </section>
      <CommentThread entityType="TASK" entityId={params.taskId} canWrite={canWrite} />
      <TaskFormDialog
        open={editOpen}
        title="Edit task"
        submitLabel="Save"
        formKey={task?.id ?? "edit"}
        initial={task ? {
          name: task.name,
          description: task.description ?? "",
          priority: formPriority(task.priority),
          dueDate: dueDateToInput(task.dueDate),
        } : null}
        error={formError}
        onClose={() => { setEditOpen(false); setFormError(null); }}
        onSubmit={onSave}
      />
    </div>
  );
}
