"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { PageHeader } from "@/components/layout/page-header";
import { FormField } from "@/components/forms/form-field";
import { InlineAlert } from "@/components/feedback/inline-alert";
import { DataTable, type DataColumn } from "@/components/data/data-table";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { cancelInvitation, createInvitation, listOrgInvitations, type Invitation } from "@/lib/api/collab";
import { getOrganization } from "@/lib/api/platform";
import { ApiError } from "@/lib/api/client";
import { requireOrgSession } from "@/lib/auth/require-org";
import { canManageOrg } from "@/lib/auth/permissions";
import { Icons } from "@/lib/icons";

const schema = z.object({ email: z.string().email("Enter a valid email") });

export default function OrgInvitationsPage() {
  const router = useRouter();
  const [orgId, setOrgId] = useState<string | null>(null);
  const [rows, setRows] = useState<Invitation[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [allowed, setAllowed] = useState(false);
  const [page, setPage] = useState(0);
  const { register, handleSubmit, reset, formState } = useForm({
    resolver: zodResolver(schema),
    defaultValues: { email: "" },
  });

  async function load(id: string) {
    const result = await listOrgInvitations(id);
    setRows(result.content ?? []);
  }

  useEffect(() => {
    const s = requireOrgSession();
    if (!s?.organizationId) {
      router.replace("/select-org");
      return;
    }
    setOrgId(s.organizationId);
    getOrganization(s.organizationId)
      .then((o) => setAllowed(canManageOrg(o.roleId)))
      .catch(() => setAllowed(false));
    load(s.organizationId).catch((e) => setError(e instanceof ApiError ? e.message : "Load failed"));
  }, [router]);

  const onSubmit = handleSubmit(async (values) => {
    if (!orgId) return;
    setError(null);
    try {
      await createInvitation({ email: values.email, organizationId: orgId });
      reset();
      await load(orgId);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Invite failed");
    }
  });

  const columns: DataColumn<Invitation>[] = [
    { id: "email", header: "Email", cell: (r) => r.email },
    { id: "status", header: "Status", cell: (r) => r.status },
    { id: "expires", header: "Expires", cell: (r) => r.expiresAt },
    {
      id: "actions",
      header: "",
      cell: (r) =>
        allowed && r.status === "PENDING" ? (
          <Button
            type="button"
            size="sm"
            variant="destructive"
            onClick={() => {
              void cancelInvitation(r.id)
                .then(() => {
                  if (orgId) return load(orgId);
                })
                .catch((e) => setError(e instanceof ApiError ? e.message : "Cancel failed"));
            }}
          >
            Cancel
          </Button>
        ) : null,
    },
  ];

  return (
    <div className="space-y-4">
      <PageHeader title="Invitations" description="Invite someone into this organization by email." icon={Icons.mail} />
      {error ? <InlineAlert>{error}</InlineAlert> : null}
      {allowed ? (
        <form className="flex max-w-lg items-end gap-2" onSubmit={onSubmit}>
          <FormField label="Email" htmlFor="email" error={formState.errors.email?.message} className="flex-1">
            <Input id="email" type="email" {...register("email")} />
          </FormField>
          <Button type="submit" disabled={formState.isSubmitting}>Send</Button>
        </form>
      ) : (
        <p className="text-sm text-muted-foreground">Only organization admins can send invites.</p>
      )}
      <DataTable
        columns={columns}
        rows={rows.slice(page * 8, page * 8 + 8)}
        page={page}
        pageSize={8}
        total={rows.length}
        onPageChange={setPage}
        getRowKey={(r) => r.id}
        emptyTitle="No invitations"
        emptyIcon={Icons.mail}
      />
    </div>
  );
}
