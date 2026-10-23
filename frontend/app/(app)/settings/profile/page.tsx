"use client";

import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { PageHeader } from "@/components/layout/page-header";
import { FormField } from "@/components/forms/form-field";
import { InlineAlert } from "@/components/feedback/inline-alert";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { getMyProfile, updateMyProfile } from "@/lib/api/auth";
import { ApiError } from "@/lib/api/client";
import { getSession, setSession } from "@/lib/auth/session";
import { Icons } from "@/lib/icons";

const schema = z.object({
  name: z.string().min(1, "Name is required").max(255),
  avatarUrl: z.string().max(500).optional(),
});

export default function ProfilePage() {
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);
  const { register, handleSubmit, reset, formState } = useForm({
    resolver: zodResolver(schema),
    defaultValues: { name: "", avatarUrl: "" },
  });

  useEffect(() => {
    getMyProfile()
      .then((p) => reset({ name: p.name, avatarUrl: p.avatarUrl ?? "" }))
      .catch((e) => setError(e instanceof ApiError ? e.message : "Could not load profile"));
  }, [reset]);

  const onSubmit = handleSubmit(async (values) => {
    setError(null);
    setSaved(false);
    try {
      const p = await updateMyProfile({
        name: values.name,
        avatarUrl: values.avatarUrl || undefined,
      });
      const session = getSession();
      if (session) {
        setSession({ ...session, user: { ...session.user, name: p.name, email: p.email } });
      }
      setSaved(true);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Save failed");
    }
  });

  return (
    <div>
      <PageHeader title="Profile" description="Name and avatar from your account." icon={Icons.settings} />
      <form className="max-w-md space-y-4" onSubmit={onSubmit}>
        <FormField label="Name" htmlFor="name" error={formState.errors.name?.message}>
          <Input id="name" {...register("name")} />
        </FormField>
        <FormField label="Avatar URL" htmlFor="avatarUrl" error={formState.errors.avatarUrl?.message}>
          <Input id="avatarUrl" {...register("avatarUrl")} />
        </FormField>
        {saved ? <InlineAlert variant="success">Profile saved.</InlineAlert> : null}
        {error ? <InlineAlert>{error}</InlineAlert> : null}
        <Button type="submit" disabled={formState.isSubmitting}>Save</Button>
      </form>
    </div>
  );
}
