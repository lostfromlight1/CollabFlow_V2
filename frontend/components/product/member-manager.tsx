"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { Users } from "lucide-react";
import { DataTable, type DataColumn } from "@/components/data/data-table";
import { FormField } from "@/components/forms/form-field";
import { InlineAlert } from "@/components/feedback/inline-alert";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { RoleChip, roleTone } from "@/components/ui/role-chip";
import { ApiError } from "@/lib/api/client";
import type { Member, RoleOption } from "@/lib/api/platform";

const schema = z.object({
  userId: z.string().uuid("Enter a user id"),
  roleId: z.string().min(1, "Pick a role"),
});

type FormValues = z.infer<typeof schema>;

export function MemberManager({
  members,
  roles,
  canManage,
  ownerRoleId,
  onAdd,
  onRole,
  onRemove,
}: {
  members: Member[];
  roles: RoleOption[];
  canManage: boolean;
  /** Role id that cannot be assigned or removed here (org owner). */
  ownerRoleId?: string;
  onAdd: (input: { userId: string; roleId: string }) => Promise<void>;
  onRole: (memberId: string, roleId: string) => Promise<void>;
  onRemove: (memberId: string) => Promise<void>;
}) {
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const pageSize = 8;
  const {
    register,
    handleSubmit,
    reset,
    formState: { isSubmitting, errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { userId: "", roleId: roles[0]?.id ?? "" },
  });

  const columns: DataColumn<Member>[] = [
    {
      id: "name",
      header: "Member",
      cell: (row) => (
        <div>
          <p className="font-medium">{row.name || "Unknown user"}</p>
          <p className="text-xs text-muted-foreground">{row.email || row.userId}</p>
        </div>
      ),
    },
    {
      id: "role",
      header: "Role",
      cell: (row) => <RoleChip role={roleTone(row.roleName)} label={row.roleName} />,
    },
    {
      id: "actions",
      header: "",
      cell: (row) => {
        const locked = ownerRoleId && row.roleId === ownerRoleId;
        if (!canManage || locked) {
          return locked ? (
            <span className="text-xs text-muted-foreground">Transfer only</span>
          ) : null;
        }
        return (
          <div className="flex items-center gap-2">
            <select
              className="h-8 rounded-lg border border-input bg-background px-2 text-xs"
              value={row.roleId}
              aria-label={`Role for ${row.name || row.userId}`}
              onChange={(e) => {
                void onRole(row.id, e.target.value).catch((err) =>
                  setError(err instanceof ApiError ? err.message : "Role update failed")
                );
              }}
            >
              {roles
                .filter((r) => r.id !== ownerRoleId)
                .map((r) => (
                  <option key={r.id} value={r.id}>
                    {r.name}
                  </option>
                ))}
            </select>
            <Button
              type="button"
              variant="destructive"
              size="sm"
              onClick={() => {
                if (!window.confirm("Remove this member?")) return;
                void onRemove(row.id).catch((err) =>
                  setError(err instanceof ApiError ? err.message : "Remove failed")
                );
              }}
            >
              Remove
            </Button>
          </div>
        );
      },
    },
  ];

  const onSubmit = handleSubmit(
    async (values) => {
      setError(null);
      try {
        await onAdd(values);
        reset();
      } catch (e) {
        setError(e instanceof ApiError ? e.message : "Could not add member");
      }
    },
    () => setError("Check the user id and role")
  );

  const start = page * pageSize;

  return (
    <div className="space-y-4">
      {canManage ? (
        <form className="grid gap-3 rounded-xl border border-border/60 bg-card p-4 sm:grid-cols-3" onSubmit={onSubmit}>
          <FormField label="User id" htmlFor="userId" error={errors.userId?.message}>
            <Input id="userId" {...register("userId")} placeholder="Existing user UUID" />
          </FormField>
          <FormField label="Role" htmlFor="roleId" error={errors.roleId?.message}>
            <select
              id="roleId"
              className="h-8 w-full rounded-lg border border-input bg-background px-2 text-sm"
              {...register("roleId")}
            >
              {roles
                .filter((r) => r.id !== ownerRoleId)
                .map((r) => (
                  <option key={r.id} value={r.id}>
                    {r.name}
                  </option>
                ))}
            </select>
          </FormField>
          <div className="flex items-end">
            <Button type="submit" disabled={isSubmitting}>
              Add member
            </Button>
          </div>
        </form>
      ) : (
        <p className="text-sm text-muted-foreground">You can view members. Managing them needs an admin role.</p>
      )}
      {error ? <InlineAlert>{error}</InlineAlert> : null}
      <DataTable
        columns={columns}
        rows={members.slice(start, start + pageSize)}
        page={page}
        pageSize={pageSize}
        total={members.length}
        onPageChange={setPage}
        getRowKey={(r) => r.id}
        emptyTitle="No members"
        emptyDescription="Members appear after they are added or accept an invite."
        emptyIcon={Users}
      />
    </div>
  );
}