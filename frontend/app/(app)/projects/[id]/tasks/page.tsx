"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { PageHeader } from "@/components/layout/page-header";
import { ProjectNav } from "@/components/product/project-nav";
import { TaskFormDialog, type TaskFormValues } from "@/components/product/task-form-dialog";
import { InlineAlert } from "@/components/feedback/inline-alert";
import { DataTable, type DataColumn } from "@/components/data/data-table";
import { Button } from "@/components/ui/button";
import { PriorityChip } from "@/components/ui/priority-chip";
import { StatusChip } from "@/components/ui/status-chip";
import { listProjectMembers, type Member } from "@/lib/api/platform";
import {
  createTask,
  deleteTask,
  dueDateFromInput,
  dueDateToInput,
  getDefaultWorkflow,
  listTasks,
  updateTask,
  type Task,
  type WorkflowStatus,
} from "@/lib/api/tasks";
import { errorText } from "@/lib/api/client";
import { ConfirmDialog } from "@/components/feedback/confirm-dialog";
import { getSession } from "@/lib/auth/session";
import { canManageProject, canWriteTasks } from "@/lib/auth/permissions";
import { priorityTone, statusTone } from "@/lib/task-meta";
import { Icons } from "@/lib/icons";

function formPriority(value: string | null | undefined): TaskFormValues["priority"] {
  if (value === "LOW" || value === "HIGH" || value === "CRITICAL") return value;
  return "MEDIUM";
}

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
  const [formError, setFormError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<Task | null>(null);
  const [deletingId, setDeletingId] = useState<string | null>(null);

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
      .catch((e) => setError(errorText(e, "Load failed")));
  }, [params.id]);

  useEffect(() => {
    load().catch((e) => setError(errorText(e, "Load failed")));
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
      cell: (r) => (
        <div className="flex justify-end gap-2">
          {canWrite ? (
            <Button type="button" size="sm" variant="outline" onClick={() => { setFormError(null); setShowForm(false); setEditing(r); }}>
              Edit
            </Button>
          ) : null}
          {canDelete ? (
            <Button type="button" size="sm" variant="destructive" onClick={() => setDeletingId(r.id)}>
              Delete
            </Button>
          ) : null}
        </div>
      ),
    },
  ];

  const closeForm = () => {
    setShowForm(false);
    setEditing(null);
    setFormError(null);
  };

  const onSubmit = async (values: TaskFormValues) => {
    setFormError(null);
    try {
      if (editing) {
        await updateTask(editing.id, {
          name: values.name,
          description: values.description,
          priority: values.priority,
          dueDate: dueDateFromInput(values.dueDate ?? "") ?? null,
        });
      } else {
        await createTask({
          projectId: params.id,
          name: values.name,
          description: values.description,
          priority: values.priority,
          dueDate: dueDateFromInput(values.dueDate ?? ""),
        });
      }
      closeForm();
      await load();
    } catch (e) {
      setFormError(errorText(e, editing ? "Save failed" : "Create failed"));
    }
  };

  return (
    <div className="space-y-4">
      <ProjectNav projectId={params.id} />
      <PageHeader
        title="Tasks"
        description="Filter, page, create, and open a task."
        icon={Icons.projects}
        actions={
          canWrite ? (
            <Button type="button" onClick={() => { setEditing(null); setFormError(null); setShowForm(true); }}>New task</Button>
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
      <TaskFormDialog
        open={showForm || editing !== null}
        title={editing ? "Edit task" : "New task"}
        submitLabel={editing ? "Save" : "Create"}
        formKey={editing?.id ?? "new"}
        initial={editing ? {
          name: editing.name,
          description: editing.description ?? "",
          priority: formPriority(editing.priority),
          dueDate: dueDateToInput(editing.dueDate),
        } : null}
        error={formError}
        onClose={closeForm}
        onSubmit={onSubmit}
      />
      <ConfirmDialog
        open={deletingId !== null}
        title="Delete task"
        message="This task is deleted and leaves the board."
        confirmLabel="Delete"
        onClose={() => setDeletingId(null)}
        onConfirm={() => {
          const id = deletingId;
          setDeletingId(null);
          if (!id) return;
          void deleteTask(id).then(load).catch((e) => setError(errorText(e, "Delete failed")));
        }}
      />
    </div>
  );
}
