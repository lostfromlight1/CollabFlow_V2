"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { FormField } from "@/components/forms/form-field";
import { InlineAlert } from "@/components/feedback/inline-alert";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { createInvitation } from "@/lib/api/collab";
import { errorText } from "@/lib/api/client";

const schema = z.object({ email: z.string().email("Enter a valid email") });

export function EmailInviteForm({
  canSend,
  organizationId,
  invitationType,
  workspaceId,
  projectId,
  hint,
}: {
  canSend: boolean;
  organizationId: string;
  invitationType: "WORKSPACE" | "PROJECT";
  workspaceId: string;
  projectId?: string;
  hint: string;
}) {
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);
  const { register, handleSubmit, reset, formState } = useForm({
    resolver: zodResolver(schema),
    defaultValues: { email: "" },
  });

  if (!canSend) return null;

  const onSubmit = handleSubmit(async (values) => {
    setError(null);
    setInfo(null);
    try {
      await createInvitation({
        email: values.email,
        organizationId,
        invitationType,
        workspaceId,
        projectId,
      });
      reset();
      setInfo("Invitation sent.");
    } catch (e) {
      setError(errorText(e, "Invite failed"));
    }
  });

  return (
    <form className="flex max-w-lg flex-col gap-2" onSubmit={onSubmit}>
      <p className="text-sm text-muted-foreground">{hint}</p>
      {error ? <InlineAlert>{error}</InlineAlert> : null}
      {info ? <InlineAlert variant="success">{info}</InlineAlert> : null}
      <div className="flex items-end gap-2">
        <FormField label="Email" htmlFor="invite-email" error={formState.errors.email?.message} className="flex-1">
          <Input id="invite-email" type="email" {...register("email")} />
        </FormField>
        <Button type="submit" disabled={formState.isSubmitting}>Send invite</Button>
      </div>
    </form>
  );
}
