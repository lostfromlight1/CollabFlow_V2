"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
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
import { createSprint, listSprints, type Sprint } from "@/lib/api/tasks";
import { ApiError } from "@/lib/api/client";
import { getSession } from "@/lib/auth/session";
import { canWriteTasks } from "@/lib/auth/permissions";
import { Icons } from "@/lib/icons";

const schema = z.object({
  name: z.string().min(1).max(255),
  goal: z.string().max(10000).optional(),
  startDate: z.string().min(1, "Start date"),
  endDate: z.string().min(1, "End date"),
});

export default function SprintsPage() {
  const params = useParams<{ id: string }>();
  const [rows, setRows] = useState<Sprint[]>([]);
  const [canWrite, setCanWrite] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [open, setOpen] = useState(false);
  const { register, handleSubmit, reset, formState } = useForm({
    resolver: zodResolver(schema),
    defaultValues: { name: "", goal: "", startDate: "", endDate: "" },
  });

  async function load() {
    setRows(await listSprints(params.id));
  }

  useEffect(() => {
    const me = getSession()?.user.id;
    listProjectMembers(params.id)
      .then((members) => setCanWrite(canWriteTasks(members.find((m) => m.userId === me)?.roleId)))
      .catch(() => setCanWrite(false));
    load().catch((e) => setError(e instanceof ApiError ? e.message : "Load failed"));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [params.id]);

  const onCreate = handleSubmit(async (values) => {
    setError(null);
    try {
      await createSprint({
        projectId: params.id,
        name: values.name,
        goal: values.goal,
        startDate: new Date(values.startDate).toISOString(),
        endDate: new Date(values.endDate).toISOString(),
      });
      reset();
      setOpen(false);
      await load();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Create failed");
    }
  });

  return (
    <div className="space-y-4">
      <ProjectNav projectId={params.id} />
      <PageHeader
        title="Sprints"
        icon={Icons.projects}
        actions={canWrite ? <Button type="button" onClick={() => setOpen((v) => !v)}>New sprint</Button> : null}
      />
      {error ? <InlineAlert>{error}</InlineAlert> : null}
      {open && canWrite ? (
        <form className="grid max-w-xl gap-3 rounded-xl border border-border/60 p-4" onSubmit={onCreate}>
          <FormField label="Name" htmlFor="name" error={formState.errors.name?.message}>
            <Input id="name" {...register("name")} />
          </FormField>
          <FormField label="Goal" htmlFor="goal">
            <Input id="goal" {...register("goal")} />
          </FormField>
          <FormField label="Start" htmlFor="startDate" error={formState.errors.startDate?.message}>
            <Input id="startDate" type="datetime-local" {...register("startDate")} />
          </FormField>
          <FormField label="End" htmlFor="endDate" error={formState.errors.endDate?.message}>
            <Input id="endDate" type="datetime-local" {...register("endDate")} />
          </FormField>
          <Button type="submit" disabled={formState.isSubmitting}>Create</Button>
        </form>
      ) : null}
      <ul className="space-y-2">
        {rows.map((s) => (
          <li key={s.id}>
            <Link href={`/projects/${params.id}/sprints/${s.id}`} className="interactive-hover-lift flex items-center justify-between rounded-xl border border-border/60 bg-card px-4 py-3">
              <span className="font-medium">{s.name}</span>
              <Badge variant="secondary">{s.status}</Badge>
            </Link>
          </li>
        ))}
        {rows.length === 0 ? <p className="text-sm text-muted-foreground">No sprints yet.</p> : null}
      </ul>
    </div>
  );
}
