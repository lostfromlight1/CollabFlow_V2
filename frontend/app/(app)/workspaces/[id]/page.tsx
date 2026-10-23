"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowLeft, FolderKanban, Plus } from "lucide-react";
import { FormField } from "@/components/forms/form-field";
import { EmptyState } from "@/components/feedback/empty-state";
import { InlineAlert } from "@/components/feedback/inline-alert";
import {
  DelayedSkeletonGate,
  ListSkeleton,
} from "@/components/feedback/skeleton";
import { PageHeader } from "@/components/layout/page-header";
import { StaggerItem, StaggerList } from "@/components/motion/stagger-list";
import { Badge } from "@/components/ui/badge";
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
  createProject,
  getWorkspace,
  listProjects,
  type Project,
  type Workspace,
} from "@/lib/api/platform";
import { requireOrgSession } from "@/lib/auth/require-org";
import { Icons } from "@/lib/icons";

const schema = z.object({
  name: z.string().min(1).max(255),
  description: z.string().max(2000).optional(),
});

type FormValues = z.infer<typeof schema>;

export default function WorkspaceDetailPage() {
  const params = useParams();
  const workspaceId = String(params.id || "");
  const router = useRouter();

  const [workspace, setWorkspace] = useState<Workspace | null>(null);
  const [projects, setProjects] = useState<Project[]>([]);
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

  async function load() {
    if (!requireOrgSession()) {
      router.replace("/select-org");
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const [ws, prjs] = await Promise.all([
        getWorkspace(workspaceId),
        listProjects(workspaceId),
      ]);
      setWorkspace(ws);
      setProjects(prjs);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Failed to load workspace");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load();
    // eslint-disable-next-line react-hooks/exhaustive-deps -- reload when id changes
  }, [workspaceId, router]);

  const onCreate = handleSubmit(
    async (values) => {
      setError(null);
      try {
        const p = await createProject({
          workspaceId,
          name: values.name,
          description: values.description,
        });
        reset();
        setShowForm(false);
        setProjects((prev) => [p, ...prev]);
      } catch (e) {
        setError(e instanceof ApiError ? e.message : "Create project failed");
      }
    },
    () => setError("Check the form fields")
  );

  return (
    <div className="space-y-6">
      <div>
        <Link
          href="/workspaces"
          className="interactive-hover-link mb-3 inline-flex items-center gap-1 text-sm text-muted-foreground"
        >
          <Icon icon={ArrowLeft} size="md" />
          Workspaces
        </Link>
        <PageHeader
          title={workspace?.name || "Workspace"}
          description={
            workspace?.description ||
            "Projects in this workspace (PRJ-002 / PRJ-001)."
          }
          icon={Icons.workspaces}
          actions={
            <div className="flex flex-wrap gap-2">
              <Link href={`/workspaces/${workspaceId}/members`} className="interactive-hover-link text-sm">
                Members
              </Link>
              <Link href={`/workspaces/${workspaceId}/settings`} className="interactive-hover-link text-sm">
                Settings
              </Link>
              <Button type="button" onClick={() => setShowForm((v) => !v)}>
                <Icon icon={Plus} size="md" />
                New project
              </Button>
            </div>
          }
        />
      </div>

      {showForm ? (
        <Card className="max-w-md border-border/60 p-4">
          <form className="flex flex-col gap-3" onSubmit={onCreate}>
            <FormField label="Name" htmlFor="name" error={errors.name?.message}>
              <Input id="name" {...register("name")} placeholder="CollabFlow MVP" />
            </FormField>
            <FormField label="Description" htmlFor="description">
              <Input id="description" {...register("description")} />
            </FormField>
            <div className="flex gap-2">
              <Button type="submit" disabled={isSubmitting}>
                Create project
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

      <DelayedSkeletonGate loading={loading} skeleton={<ListSkeleton rows={3} />}>
        {!loading && projects.length === 0 ? (
          <EmptyState
            title="No projects yet"
            description="Create a project to start tracking tasks."
            icon={FolderKanban}
            action={
              <Button type="button" onClick={() => setShowForm(true)}>
                <Icon icon={Plus} size="md" />
                Create project
              </Button>
            }
          />
        ) : null}
        <StaggerList className="grid gap-3 sm:grid-cols-2">
          {projects.map((p, i) => (
            <StaggerItem key={p.id} index={i}>
              <Link href={`/projects/${p.id}`} className="block">
              <Card className="interactive-hover-lift border-border/60">
                <CardHeader>
                  <div className="mb-1 flex flex-wrap items-center gap-2">
                    <CardTitle>{p.name}</CardTitle>
                    {p.status ? (
                      <Badge variant="secondary">{p.status}</Badge>
                    ) : null}
                    {p.projectType ? (
                      <Badge variant="outline">{p.projectType}</Badge>
                    ) : null}
                  </div>
                  <CardDescription>
                    {p.description || "No description"}
                  </CardDescription>
                  <p className="pt-2 font-mono text-xs text-muted-foreground">
                    {p.id}
                  </p>
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
