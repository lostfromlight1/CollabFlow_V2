"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { PageHeader } from "@/components/layout/page-header";
import { ProjectNav } from "@/components/product/project-nav";
import { FormField } from "@/components/forms/form-field";
import { InlineAlert } from "@/components/feedback/inline-alert";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { listProjectMembers } from "@/lib/api/platform";
import {
  attachSprintTask,
  completeSprint,
  deleteSprint,
  detachSprintTask,
  getSprint,
  listTasks,
  startSprint,
  updateSprint,
  type Sprint,
  type Task,
} from "@/lib/api/tasks";
import { ApiError } from "@/lib/api/client";
import { getSession } from "@/lib/auth/session";
import { canManageProject, canWriteTasks } from "@/lib/auth/permissions";
import { Icons } from "@/lib/icons";

const schema = z.object({
  name: z.string().min(1).max(255),
  goal: z.string().max(10000).optional(),
  taskId: z.string().optional(),
});

export default function SprintDetailPage() {
  const params = useParams<{ id: string; sprintId: string }>();
  const router = useRouter();
  const [sprint, setSprint] = useState<Sprint | null>(null);
  const [tasks, setTasks] = useState<Task[]>([]);
  const [canWrite, setCanWrite] = useState(false);
  const [canDelete, setCanDelete] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { register, handleSubmit, reset, getValues, formState } = useForm({
    resolver: zodResolver(schema),
    defaultValues: { name: "", goal: "", taskId: "" },
  });

  async function load() {
    const [s, page] = await Promise.all([
      getSprint(params.sprintId),
      listTasks({ projectId: params.id, sprintId: params.sprintId, size: 100 }),
    ]);
    setSprint(s);
    setTasks(page.content);
    reset({ name: s.name, goal: s.goal ?? "", taskId: "" });
  }

  useEffect(() => {
    const me = getSession()?.user.id;
    listProjectMembers(params.id)
      .then((members) => {
        const role = members.find((m) => m.userId === me)?.roleId;
        setCanWrite(canWriteTasks(role));
        setCanDelete(canManageProject(role));
      })
      .catch(() => undefined);
    load().catch((e) => setError(e instanceof ApiError ? e.message : "Load failed"));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [params.id, params.sprintId]);

  const onSave = handleSubmit(async (values) => {
    setError(null);
    try {
      setSprint(await updateSprint(params.sprintId, { name: values.name, goal: values.goal }));
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Save failed");
    }
  });

  return (
    <div className="space-y-4">
      <ProjectNav projectId={params.id} />
      <PageHeader
        title={sprint?.name || "Sprint"}
        description={sprint?.goal || "Start, complete, and attach tasks."}
        icon={Icons.projects}
        badge={sprint ? <Badge variant="secondary">{sprint.status}</Badge> : null}
      />
      {error ? <InlineAlert>{error}</InlineAlert> : null}
      <form className="max-w-lg space-y-3" onSubmit={onSave}>
        <FormField label="Name" htmlFor="name" error={formState.errors.name?.message}>
          <Input id="name" disabled={!canWrite} {...register("name")} />
        </FormField>
        <FormField label="Goal" htmlFor="goal">
          <Input id="goal" disabled={!canWrite} {...register("goal")} />
        </FormField>
        {canWrite ? <Button type="submit" disabled={formState.isSubmitting}>Save</Button> : null}
      </form>
      <div className="flex flex-wrap gap-2">
        {canWrite ? (
          <>
            <Button type="button" variant="outline" onClick={() => void startSprint(params.sprintId).then(setSprint).catch((e) => setError(e instanceof ApiError ? e.message : "Start failed"))}>Start</Button>
            <Button type="button" variant="outline" onClick={() => void completeSprint(params.sprintId).then(setSprint).catch((e) => setError(e instanceof ApiError ? e.message : "Complete failed"))}>Complete</Button>
          </>
        ) : null}
        {canDelete ? (
          <Button
            type="button"
            variant="destructive"
            onClick={() => {
              if (!window.confirm("Delete this sprint?")) return;
              void deleteSprint(params.sprintId)
                .then(() => router.push(`/projects/${params.id}/sprints`))
                .catch((e) => setError(e instanceof ApiError ? e.message : "Delete failed"));
            }}
          >
            Delete
          </Button>
        ) : null}
      </div>
      <section className="space-y-2">
        <h2 className="text-sm font-medium">Tasks in this sprint</h2>
        <ul className="space-y-1 text-sm">
          {tasks.map((t) => (
            <li key={t.id} className="flex items-center justify-between gap-2">
              <span>{t.name}</span>
              {canWrite ? (
                <Button type="button" size="sm" variant="outline" onClick={() => void detachSprintTask(params.sprintId, t.id).then(load)}>
                  Detach
                </Button>
              ) : null}
            </li>
          ))}
        </ul>
        {canWrite ? (
          <div className="flex gap-2">
            <Input placeholder="Task id" {...register("taskId")} />
            <Button
              type="button"
              variant="outline"
              onClick={() => {
                const taskId = getValues("taskId")?.trim();
                if (!taskId) return;
                void attachSprintTask(params.sprintId, taskId)
                  .then(load)
                  .catch((e) => setError(e instanceof ApiError ? e.message : "Attach failed"));
              }}
            >
              Attach
            </Button>
          </div>
        ) : null}
      </section>
    </div>
  );
}
