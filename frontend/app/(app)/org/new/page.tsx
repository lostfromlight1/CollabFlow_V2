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
import { createOrganization } from "@/lib/api/platform";
import { ApiError } from "@/lib/api/client";
import { Icons } from "@/lib/icons";

const schema = z.object({
  name: z.string().min(1).max(255),
  slug: z.string().regex(/^[a-z0-9]+(?:-[a-z0-9]+)*$/, "Lowercase letters, numbers, and hyphens"),
  description: z.string().max(2000).optional(),
});

export default function NewOrgPage() {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const { register, handleSubmit, formState } = useForm({
    resolver: zodResolver(schema),
    defaultValues: { name: "", slug: "", description: "" },
  });

  const onSubmit = handleSubmit(async (values) => {
    setError(null);
    try {
      await createOrganization(values);
      router.push("/select-org");
    } catch (e) {
      setError(
        e instanceof ApiError
          ? e.message
          : "Create failed. Self-serve org create may be turned off — use the admin portal."
      );
    }
  });

  return (
    <div>
      <PageHeader
        title="New organization"
        description="Creates an organization when the server allows self-serve. Otherwise a master admin creates it."
        icon={Icons.organizations}
      />
      <form className="max-w-md space-y-3" onSubmit={onSubmit}>
        <FormField label="Name" htmlFor="name" error={formState.errors.name?.message}>
          <Input id="name" {...register("name")} />
        </FormField>
        <FormField label="Slug" htmlFor="slug" error={formState.errors.slug?.message}>
          <Input id="slug" {...register("slug")} placeholder="kaung-corp" />
        </FormField>
        <FormField label="Description" htmlFor="description">
          <Input id="description" {...register("description")} />
        </FormField>
        {error ? <InlineAlert>{error}</InlineAlert> : null}
        <Button type="submit" disabled={formState.isSubmitting}>Create</Button>
      </form>
    </div>
  );
}
