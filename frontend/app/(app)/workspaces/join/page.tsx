"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { PageHeader } from "@/components/layout/page-header";
import { FormField } from "@/components/forms/form-field";
import { InlineAlert } from "@/components/feedback/inline-alert";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { joinWorkspace } from "@/lib/api/platform";
import { ApiError } from "@/lib/api/client";
import { Icons } from "@/lib/icons";

const schema = z.object({
  workspaceId: z.string().uuid("Workspace id"),
  inviteCode: z.string().min(6).max(20),
});

export default function JoinWorkspacePage() {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const { register, handleSubmit, formState } = useForm({
    resolver: zodResolver(schema),
    defaultValues: { workspaceId: "", inviteCode: "" },
  });

  const onSubmit = handleSubmit(async (values) => {
    setError(null);
    try {
      await joinWorkspace(values.workspaceId, values.inviteCode);
      router.push(`/workspaces/${values.workspaceId}`);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Join failed");
    }
  });

  return (
    <div>
      <PageHeader title="Join workspace" description="Use the invite code from a workspace admin." icon={Icons.workspaces} />
      <form className="max-w-md space-y-3" onSubmit={onSubmit}>
        <FormField label="Workspace id" htmlFor="workspaceId" error={formState.errors.workspaceId?.message}>
          <Input id="workspaceId" {...register("workspaceId")} />
        </FormField>
        <FormField label="Invite code" htmlFor="inviteCode" error={formState.errors.inviteCode?.message}>
          <Input id="inviteCode" {...register("inviteCode")} />
        </FormField>
        {error ? <InlineAlert>{error}</InlineAlert> : null}
        <Button type="submit" disabled={formState.isSubmitting}>Join</Button>
      </form>
    </div>
  );
}
