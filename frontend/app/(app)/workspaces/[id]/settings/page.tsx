"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { PageHeader } from "@/components/layout/page-header";
import { FormField } from "@/components/forms/form-field";
import { InlineAlert } from "@/components/feedback/inline-alert";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  deleteWorkspace,
  getWorkspace,
  listWorkspaceMembers,
  updateWorkspace,
  type Workspace,
} from "@/lib/api/platform";
import { ApiError } from "@/lib/api/client";
import { getSession } from "@/lib/auth/session";
import { canManageWorkspace } from "@/lib/auth/permissions";
import { Icons } from "@/lib/icons";

const schema = z.object({
  name: z.string().min(1).max(255),
  description: z.string().max(2000).optional(),
});

export default function WorkspaceSettingsPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const [ws, setWs] = useState<Workspace | null>(null);
  const [canManage, setCanManage] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);
  const { register, handleSubmit, reset, formState } = useForm({
    resolver: zodResolver(schema),
    defaultValues: { name: "", description: "" },
  });

  useEffect(() => {
    const me = getSession()?.user.id;
    Promise.all([getWorkspace(params.id), listWorkspaceMembers(params.id)])
      .then(([workspace, members]) => {
        setWs(workspace);
        const mine = members.find((m) => m.userId === me);
        setCanManage(canManageWorkspace(mine?.roleId));
        reset({ name: workspace.name, description: workspace.description ?? "" });
      })
      .catch((e) => setError(e instanceof ApiError ? e.message : "Load failed"));
  }, [params.id, reset]);

  const onSave = handleSubmit(async (values) => {
    setError(null);
    try {
      setWs(await updateWorkspace(params.id, values));
      setInfo("Workspace saved.");
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Save failed");
    }
  });

  return (
    <div className="space-y-4">
      <PageHeader title={ws?.name || "Workspace settings"} description="Name, description, and invite code." icon={Icons.workspaces} />
      <Link href={`/workspaces/${params.id}`} className="interactive-hover-link text-sm text-muted-foreground">Back to workspace</Link>
      {error ? <InlineAlert>{error}</InlineAlert> : null}
      {info ? <InlineAlert variant="success">{info}</InlineAlert> : null}
      {ws?.inviteCode ? <p className="text-sm">Invite code: <span className="font-mono">{ws.inviteCode}</span></p> : null}
      <form className="max-w-md space-y-3" onSubmit={onSave}>
        <FormField label="Name" htmlFor="name" error={formState.errors.name?.message}>
          <Input id="name" disabled={!canManage} {...register("name")} />
        </FormField>
        <FormField label="Description" htmlFor="description">
          <Input id="description" disabled={!canManage} {...register("description")} />
        </FormField>
        {canManage ? (
          <div className="flex gap-2">
            <Button type="submit" disabled={formState.isSubmitting}>Save</Button>
            <Button
              type="button"
              variant="destructive"
              onClick={() => {
                if (!window.confirm("Delete this workspace?")) return;
                void deleteWorkspace(params.id)
                  .then(() => router.push("/workspaces"))
                  .catch((e) => setError(e instanceof ApiError ? e.message : "Delete failed"));
              }}
            >
              Delete
            </Button>
          </div>
        ) : (
          <p className="text-sm text-muted-foreground">Workspace admins can edit.</p>
        )}
      </form>
    </div>
  );
}
