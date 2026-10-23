"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { PageHeader } from "@/components/layout/page-header";
import { ProjectNav } from "@/components/product/project-nav";
import { FormField } from "@/components/forms/form-field";
import { InlineAlert } from "@/components/feedback/inline-alert";
import { DataTable, type DataColumn } from "@/components/data/data-table";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { PriorityChip } from "@/components/ui/priority-chip";
import { StatusChip } from "@/components/ui/status-chip";
import { listProjectMembers, type Member } from "@/lib/api/platform";
import {
  createTask,
  deleteTask,
  getDefaultWorkflow,
  listTasks,
  type Task,
  type WorkflowStatus,
} from "@/lib/api/tasks";
import { ApiError } from "@/lib/api/client";
import { getSession } from "@/lib/auth/session";
import { canManageProject, canWriteTasks } from "@/lib/auth/permissions";
import { priorityTone, statusTone } from "@/lib/task-meta";
import { Icons } from "@/lib/icons";

const schema = z.object({
  name: z.string().min(1).max(255),
  description: z.string().max(10000).optional(),
  priority: z.enum(["LOW", "MEDIUM", "HIGH", "CRITICAL"]),
});

export default function TasksPage() {
  const params = useParams<{ id: string }>();
  const [rows, setRows] = useState<Task[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [statuses, setStatuses] = useState<WorkflowStatus[]>([]);
  const [statusId, setStatusId] = useState("");
  const [priority, setPriority] = useState("");
  const [canWrite, setCanWrite] = useState(false);
  const [canDelete, setCanDelete] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const { register, handleSubmit, reset, formState } = useForm({
    resolver: zodResolver(schema),
    defaultValues: { name: "", description: "", priority: "MEDIUM" as const },
  });

  const load = useCallback(async () => {
    const pageResult = await listTasks({
      projectId: params.id,
      page,
      size: 10,
      statusId: statusId || undefined,
      priority: priority || undefined,
    });
    setRows(pageResult.content);
    setTotal(pageResult.totalElements);
  }, [page, params.id, priority, statusId]);

  useEffect(() => {
    const me = getSession()?.user.id;
    Promise.all([getDefaultWorkflow(), listProjectMembers(params.id)])
      .then(([wf, members]) => {
        setStatuses([...wf.statuses].sort((a, b) => a.displayOrder - b.displayOrder));
        const role = members.find((m: Member) => m.userId === me)?.roleId;
        setCanWrite(canWriteTasks(role));
        setCanDelete(canManageProject(role));
      })
      .catch((e) => setError(e instanceof ApiError ? e.message : "Load failed"));
  }, [params.id]);

  useEffect(() => {
    load().catch((e) => setError(e instanceof ApiError ? e.message : "Load failed"));
  }, [load]);

  const statusById = new Map(statuses.map((s) => [s.id, s]));

  const columns: DataColumn<Task>[] = [
    {
      id: "name",
      header: "Task",
      cell: (r) => (
        <Link href={`/projects/${params.id}/tasks/${r.id}`} className="interactive-hover-link font-medium">
          {r.name}
        </Link>
      ),
    },
    {
      id: "status",
      header: "Status",
      cell: (r) => {
        const s = r.statusId ? statusById.get(r.statusId) : undefined;
        return s ? <StatusChip status={statusTone(s.name, s.category)} label={s.name} /> : "—";
      },
    },
    {
      id: "priority",
      header: "Priority",
      cell: (r) => <PriorityChip priority={priorityTone(r.priority)} />,
    },
    { id: "due", header: "Due", cell: (r) => r.dueDate?.slice(0, 10) || "—" },
    {
      id: "actions",
      header: "",
      cell: (r) =>
        canDelete ? (
          <Button
            type="button"
            size="sm"
            variant="destructive"
            onClick={() => {
              if (!window.confirm("Delete this task?")) return;
              void deleteTask(r.id).then(load).catch((e) => setError(e instanceof ApiError ? e.message : "Delete failed"));
            }}
          >
            Delete
          </Button>
        ) : null,
    },
  ];

  const onCreate = handleSubmit(async (values) => {
    setError(null);
    try {
      await createTask({ projectId: params.id, ...values });
      reset();
      setShowForm(false);
      await load();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Create failed");
    }
  });

  return (
    <div className="space-y-4">
      <ProjectNav projectId={params.id} />
      <PageHeader
        title="Tasks"
        description="Filter, page, create, and open a task."
        icon={Icons.projects}
        actions={
          canWrite ? (
            <Button type="button" onClick={() => setShowForm((v) => !v)}>New task</Button>
          ) : null
        }
      />
      <div className="flex flex-wrap gap-2">
        <select className="h-8 rounded-lg border border-input bg-background px-2 text-sm" value={statusId} onChange={(e) => { setPage(0); setStatusId(e.target.value); }}>
          <option value="">All statuses</option>
          {statuses.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
        </select>
        <select className="h-8 rounded-lg border border-input bg-background px-2 text-sm" value={priority} onChange={(e) => { setPage(0); setPriority(e.target.value); }}>
          <option value="">All priorities</option>
          <option value="LOW">Low</option>
          <option value="MEDIUM">Medium</option>
          <option value="HIGH">High</option>
          <option value="CRITICAL">Urgent</option>
        </select>
      </div>
      {showForm && canWrite ? (
        <form className="grid max-w-xl gap-3 rounded-xl border border-border/60 p-4" onSubmit={onCreate}>
          <FormField label="Name" htmlFor="name" error={formState.errors.name?.message}>
            <Input id="name" {...register("name")} />
          </FormField>
          <FormField label="Description" htmlFor="description">
            <Input id="description" {...register("description")} />
          </FormField>
          <FormField label="Priority" htmlFor="priority">
            <select id="priority" className="h-8 w-full rounded-lg border border-input bg-background px-2 text-sm" {...register("priority")}>
              <option value="LOW">Low</option>
              <option value="MEDIUM">Medium</option>
              <option value="HIGH">High</option>
              <option value="CRITICAL">Urgent</option>
            </select>
          </FormField>
          <Button type="submit" disabled={formState.isSubmitting}>Create</Button>
        </form>
      ) : null}
      {error ? <InlineAlert>{error}</InlineAlert> : null}
      <DataTable
        columns={columns}
        rows={rows}
        page={page}
        pageSize={10}
        total={total}
        onPageChange={setPage}
        getRowKey={(r) => r.id}
        emptyTitle="No tasks"
        emptyDescription="Create a task to start the board."
        emptyIcon={Icons.projects}
      />
    </div>
  );
}
