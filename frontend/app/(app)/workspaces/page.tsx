"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { LayoutGrid, Plus } from "lucide-react";
import { FormField } from "@/components/forms/form-field";
import { EmptyState } from "@/components/feedback/empty-state";
import { InlineAlert } from "@/components/feedback/inline-alert";
import {
  DelayedSkeletonGate,
  ListSkeleton,
} from "@/components/feedback/skeleton";
import { PageHeader } from "@/components/layout/page-header";
import { StaggerItem, StaggerList } from "@/components/motion/stagger-list";
import { Button } from "@/components/ui/button";
import { Icon } from "@/components/ui/icon";
import { Input } from "@/components/ui/input";
import {
  Card,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { ApiError } from "@/lib/api/client";
import {
  createWorkspace,
  listWorkspaces,
  type Workspace,
} from "@/lib/api/platform";
import { orgDisplayName, requireOrgSession } from "@/lib/auth/require-org";
import { Icons } from "@/lib/icons";

const schema = z.object({
  name: z.string().min(1, "Name is required").max(255),
  description: z.string().max(2000).optional(),
});

type FormValues = z.infer<typeof schema>;

export default function WorkspacesPage() {
  const router = useRouter();
  const [orgId, setOrgId] = useState<string | null>(null);
  const [orgName, setOrgName] = useState("");
  const [rows, setRows] = useState<Workspace[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);

  const {
    register,
    handleSubmit,
    reset,
    formState: { isSubmitting, errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { name: "", description: "" },
  });

  async function load(organizationId: string) {
    setLoading(true);
    setError(null);
    try {
      const list = await listWorkspaces(organizationId);
      setRows(list);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Failed to load workspaces");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    const s = requireOrgSession();
    if (!s) {
      router.replace("/select-org");
      return;
    }
    setOrgId(s.organizationId);
    setOrgName(orgDisplayName(s.organizationId));
    void load(s.organizationId!);
  }, [router]);

  const onCreate = handleSubmit(
    async (values) => {
      if (!orgId) return;
      setError(null);
      try {
        const ws = await createWorkspace({
          organizationId: orgId,
          name: values.name,
          description: values.description,
        });
        reset();
        setShowForm(false);
        router.push(`/workspaces/${ws.id}`);
      } catch (e) {
        setError(e instanceof ApiError ? e.message : "Create failed");
      }
    },
    () => setError("Check the form fields")
  );

  return (
    <div className="space-y-6">
      <PageHeader
        title="Workspaces"
        description={`Org: ${orgName || "…"}`}
        icon={Icons.workspaces}
        actions={
          <div className="flex items-center gap-3">
            <Link href="/workspaces/join" className="interactive-hover-link text-sm text-muted-foreground">
              Join with code
            </Link>
            <Button type="button" onClick={() => setShowForm((v) => !v)}>
              <Icon icon={Plus} size="md" />
              New workspace
            </Button>
          </div>
        }
      />

      {showForm ? (
        <Card className="max-w-md border-border/60 p-4">
          <form className="flex flex-col gap-3" onSubmit={onCreate}>
            <FormField label="Name" htmlFor="name" error={errors.name?.message}>
              <Input id="name" {...register("name")} placeholder="Engineering" />
            </FormField>
            <FormField label="Description" htmlFor="description">
              <Input id="description" {...register("description")} />
            </FormField>
            <div className="flex gap-2">
              <Button type="submit" disabled={isSubmitting}>
                Create
              </Button>
              <Button
                type="button"
                variant="outline"
                onClick={() => setShowForm(false)}
              >
                Cancel
              </Button>
            </div>
          </form>
        </Card>
      ) : null}

      {error ? <InlineAlert>{error}</InlineAlert> : null}

      <DelayedSkeletonGate loading={loading} skeleton={<ListSkeleton rows={4} />}>
        {!loading && rows.length === 0 ? (
          <EmptyState
            title="No workspaces yet"
            description="Create a workspace to hold projects and tasks."
            icon={LayoutGrid}
            action={
              <Button type="button" onClick={() => setShowForm(true)}>
                <Icon icon={Plus} size="md" />
                Create workspace
              </Button>
            }
          />
        ) : null}
        <StaggerList className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {rows.map((ws, i) => (
            <StaggerItem key={ws.id} index={i}>
              <Link href={`/workspaces/${ws.id}`} className="block">
                <Card className="interactive-hover-lift h-full border-border/60">
                  <CardHeader>
                    <CardTitle>{ws.name}</CardTitle>
                    <CardDescription>
                      {ws.description || "No description"}
                    </CardDescription>
                  </CardHeader>
                </Card>
              </Link>
            </StaggerItem>
          ))}
        </StaggerList>
      </DelayedSkeletonGate>
    </div>
  );
}
