"use client";

import { useState } from "react";
import { createPortal } from "react-dom";
import { PriorityChip } from "@/components/ui/priority-chip";
import { StatusChip } from "@/components/ui/status-chip";
import { priorityTone, statusTone } from "@/lib/task-meta";
import type { Task, WorkflowStatus } from "@/lib/api/tasks";

export function TaskPreview({
  task,
  status,
  children,
}: {
  task: Task;
  status?: WorkflowStatus;
  children: React.ReactNode;
}) {
  const [pos, setPos] = useState<{ x: number; y: number } | null>(null);

  return (
    <div
      onMouseEnter={(e) => setPos({ x: e.clientX, y: e.clientY })}
      onMouseLeave={() => setPos(null)}
    >
      {children}
      {pos && typeof document !== "undefined"
        ? createPortal(
            <div
              className="pointer-events-none fixed z-50 w-64 rounded-xl border border-border/70 bg-popover p-3 text-sm shadow-lg"
              style={{ left: Math.min(pos.x + 12, window.innerWidth - 280), top: pos.y + 12 }}
            >
              <p className="font-medium">{task.name}</p>
              <p className="mt-1 line-clamp-3 text-xs text-muted-foreground">
                {task.description || "No description"}
              </p>
              <div className="mt-2 flex flex-wrap gap-1">
                {status ? (
                  <StatusChip status={statusTone(status.name, status.category)} label={status.name} />
                ) : null}
                <PriorityChip priority={priorityTone(task.priority)} />
              </div>
            </div>,
            document.body
          )
        : null}
    </div>
  );
}
