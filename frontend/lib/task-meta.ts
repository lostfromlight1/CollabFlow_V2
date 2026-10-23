/**
 * Shared status / priority labels + chip classes (CSS tokens in globals.css).
 * Safe for real + demo UI — no demo imports.
 */
import {
  AlertCircle,
  ArrowDown,
  ArrowUp,
  CheckCircle2,
  CircleDashed,
  Flame,
  Minus,
  PauseCircle,
  Timer,
  type LucideIcon,
} from "lucide-react";

export type TaskStatus =
  | "todo"
  | "in_progress"
  | "review"
  | "done"
  | "blocked";

export type TaskPriority = "low" | "medium" | "high" | "urgent";

export const STATUS_LABEL: Record<TaskStatus, string> = {
  todo: "To Do",
  in_progress: "In Progress",
  review: "In Review",
  done: "Done",
  blocked: "Blocked",
};

export const STATUS_CHIP: Record<TaskStatus, string> = {
  todo: "chip-status-todo",
  in_progress: "chip-status-progress",
  review: "chip-status-review",
  done: "chip-status-done",
  blocked: "chip-status-blocked",
};

export const STATUS_ICON: Record<TaskStatus, LucideIcon> = {
  todo: CircleDashed,
  in_progress: Timer,
  review: PauseCircle,
  done: CheckCircle2,
  blocked: AlertCircle,
};

export const PRIORITY_LABEL: Record<TaskPriority, string> = {
  low: "Low",
  medium: "Medium",
  high: "High",
  urgent: "Urgent",
};

export const PRIORITY_CHIP: Record<TaskPriority, string> = {
  low: "chip-priority-low",
  medium: "chip-priority-medium",
  high: "chip-priority-high",
  urgent: "chip-priority-urgent",
};

export const PRIORITY_ICON: Record<TaskPriority, LucideIcon> = {
  low: ArrowDown,
  medium: Minus,
  high: ArrowUp,
  urgent: Flame,
};

export const ALL_STATUSES: TaskStatus[] = [
  "todo",
  "in_progress",
  "review",
  "done",
  "blocked",
];

export const ALL_PRIORITIES: TaskPriority[] = [
  "low",
  "medium",
  "high",
  "urgent",
];

/** Map workflow status name + category onto the shared chip set. */
export function statusTone(name: string, category?: string | null): TaskStatus {
  const n = name.toLowerCase();
  if (n.includes("block")) return "blocked";
  if (n.includes("review")) return "review";
  if (n.includes("progress")) return "in_progress";
  if (n.includes("done") || category === "DONE") return "done";
  if (category === "IN_PROGRESS") return "in_progress";
  return "todo";
}

/** API stores CRITICAL; chips use urgent. */
export function priorityTone(priority?: string | null): TaskPriority {
  switch ((priority || "MEDIUM").toUpperCase()) {
    case "LOW":
      return "low";
    case "HIGH":
      return "high";
    case "CRITICAL":
      return "urgent";
    default:
      return "medium";
  }
}
