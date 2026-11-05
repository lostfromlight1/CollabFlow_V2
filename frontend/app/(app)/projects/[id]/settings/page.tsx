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
import {
  archiveProject,
  getProject,
  listProjectMembers,
  updateProject,
  type Project,
} from "@/lib/api/platform";
import { errorText } from "@/lib/api/client";
import { ConfirmDialog } from "@/components/feedback/confirm-dialog";
import { getSession } from "@/lib/auth/session";
import { canManageProject } from "@/lib/auth/permissions";
import { Icons } from "@/lib/icons";

const schema = z.object({
  name: z.string().min(1).max(255),
  description: z.string().max(2000).optional(),
  status: z.string().min(1),
});

export default function ProjectSettingsPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const [project, setProject] = useState<Project | null>(null);
  const [canManage, setCanManage] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [archiveOpen, setArchiveOpen] = useState(false);
  const { register, handleSubmit, reset, formState } = useForm({
    resolver: zodResolver(schema),
    defaultValues: { name: "", description: "", status: "ACTIVE" },
  });

  useEffect(() => {
    const me = getSession()?.user.id;
    Promise.all([getProject(params.id), listProjectMembers(params.id)])
      .then(([p, members]) => {
        setProject(p);
        setCanManage(canManageProject(members.find((m) => m.userId === me)?.roleId));
        reset({ name: p.name, description: p.description ?? "", status: p.status });
      })
      .catch((e) => setError(errorText(e, "Load failed")));
  }, [params.id, reset]);

  const onSave = handleSubmit(async (values) => {
    setError(null);
    try {
      setProject(await updateProject(params.id, values));
    } catch (e) {
      setError(errorText(e, "Save failed"));
    }
  });

  return (
    <div className="space-y-4">
      <ProjectNav projectId={params.id} />
      <PageHeader title="Project settings" description={project?.name} icon={Icons.settings} />
      {error ? <InlineAlert>{error}</InlineAlert> : null}
      <form className="max-w-md space-y-3" onSubmit={onSave}>
        <FormField label="Name" htmlFor="name" error={formState.errors.name?.message}>
          <Input id="name" disabled={!canManage} {...register("name")} />
        </FormField>
        <FormField label="Description" htmlFor="description">
          <Input id="description" disabled={!canManage} {...register("description")} />
        </FormField>
        <FormField label="Status" htmlFor="status">
          <select id="status" disabled={!canManage} className="h-8 w-full rounded-lg border border-input bg-background px-2 text-sm" {...register("status")}>
            <option value="ACTIVE">ACTIVE</option>
            <option value="ON_HOLD">ON_HOLD</option>
          </select>
        </FormField>
        {canManage ? (
          <div className="flex gap-2">
            <Button type="submit" disabled={formState.isSubmitting}>Save</Button>
            <Button
              type="button"
              variant="destructive"
              onClick={() => setArchiveOpen(true)}
            >
              Archive
            </Button>
          </div>
        ) : (
          <p className="text-sm text-muted-foreground">Project admins can edit.</p>
        )}
      </form>
      <ConfirmDialog
        open={archiveOpen}
        title="Archive project"
        message="This project leaves the active list. You can still open it from the workspace if the server keeps it."
        confirmLabel="Archive"
        onClose={() => setArchiveOpen(false)}
        onConfirm={() => {
          setArchiveOpen(false);
          void archiveProject(params.id)
            .then(() => router.push(`/workspaces/${project?.workspaceId ?? ""}`))
            .catch((e) => setError(errorText(e, "Archive failed")));
        }}
      />
    </div>
  );
}
