"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { PageHeader } from "@/components/layout/page-header";
import { FormField } from "@/components/forms/form-field";
import { InlineAlert } from "@/components/feedback/inline-alert";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  deleteOrganization,
  getOrganization,
  transferOrganization,
  updateOrganization,
  type Organization,
} from "@/lib/api/platform";
import { errorText } from "@/lib/api/client";
import { ConfirmDialog } from "@/components/feedback/confirm-dialog";
import { requireOrgSession } from "@/lib/auth/require-org";
import { clearSession } from "@/lib/auth/session";
import { canManageOrg, isOrgOwner } from "@/lib/auth/permissions";
import { Icons } from "@/lib/icons";

const schema = z.object({
  name: z.string().min(1).max(255),
  description: z.string().max(2000).optional(),
  newOwnerUserId: z.string().optional(),
});

export default function OrgSettingsPage() {
  const router = useRouter();
  const [org, setOrg] = useState<Organization | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const { register, handleSubmit, reset, getValues, formState } = useForm({
    resolver: zodResolver(schema),
    defaultValues: { name: "", description: "", newOwnerUserId: "" },
  });

  useEffect(() => {
    const s = requireOrgSession();
    if (!s?.organizationId) {
      router.replace("/select-org");
      return;
    }
    getOrganization(s.organizationId)
      .then((o) => {
        setOrg(o);
        reset({ name: o.name, description: o.description ?? "", newOwnerUserId: "" });
      })
      .catch((e) => setError(errorText(e, "Could not load organization")));
  }, [reset, router]);

  const manage = canManageOrg(org?.roleId);
  const owner = isOrgOwner(org?.roleId);

  const onSave = handleSubmit(async (values) => {
    if (!org) return;
    setError(null);
    try {
      const next = await updateOrganization(org.id, { name: values.name, description: values.description });
      setOrg(next);
      setInfo("Organization updated.");
    } catch (e) {
      setError(errorText(e, "Update failed"));
    }
  });

  return (
    <div className="space-y-6">
      <PageHeader title="Organization settings" description="Edit details, transfer ownership, or delete." icon={Icons.settings} />
      {error ? <InlineAlert>{error}</InlineAlert> : null}
      {info ? <InlineAlert variant="success">{info}</InlineAlert> : null}
      <form className="max-w-md space-y-3" onSubmit={onSave}>
        <FormField label="Name" htmlFor="name" error={formState.errors.name?.message}>
          <Input id="name" disabled={!manage} {...register("name")} />
        </FormField>
        <FormField label="Description" htmlFor="description">
          <Input id="description" disabled={!manage} {...register("description")} />
        </FormField>
        {manage ? <Button type="submit" disabled={formState.isSubmitting}>Save</Button> : <p className="text-sm text-muted-foreground">Only admins can edit.</p>}
      </form>
      {owner && org ? (
        <div className="max-w-md space-y-2 rounded-xl border border-border/60 p-4">
          <FormField label="New owner user id" htmlFor="newOwnerUserId">
            <Input id="newOwnerUserId" {...register("newOwnerUserId")} />
          </FormField>
          <Button
            type="button"
            variant="destructive"
            onClick={() => {
              const id = getValues("newOwnerUserId");
              if (!id) return setError("Enter the new owner's user id");
              void transferOrganization(org.id, id)
                .then((next) => {
                  setOrg(next);
                  setInfo("Ownership transferred.");
                })
                .catch((e) => setError(errorText(e, "Transfer failed")));
            }}
          >
            Transfer ownership
          </Button>
          <Button type="button" variant="destructive" onClick={() => setDeleteOpen(true)}>
            Delete organization
          </Button>
        </div>
      ) : null}
      <ConfirmDialog
        open={deleteOpen}
        title="Delete organization"
        message="This organization is deleted and you are signed out."
        confirmLabel="Delete"
        onClose={() => setDeleteOpen(false)}
        onConfirm={() => {
          if (!org) return;
          setDeleteOpen(false);
          void deleteOrganization(org.id)
            .then(() => {
              clearSession();
              router.replace("/login");
            })
            .catch((e) => setError(errorText(e, "Delete failed")));
        }}
      />
    </div>
  );
}
