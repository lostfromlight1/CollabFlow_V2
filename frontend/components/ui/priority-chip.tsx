import { Icon } from "@/components/ui/icon";
import {
  PRIORITY_CHIP,
  PRIORITY_ICON,
  PRIORITY_LABEL,
  type TaskPriority,
} from "@/lib/task-meta";
import { cn } from "@/lib/utils";

export function PriorityChip({
  priority,
  className,
  showIcon = true,
}: {
  priority: TaskPriority;
  className?: string;
  showIcon?: boolean;
}) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1 rounded-md px-2 py-0.5 text-xs font-medium",
        PRIORITY_CHIP[priority],
        className
      )}
    >
      {showIcon ? <Icon icon={PRIORITY_ICON[priority]} size="sm" /> : null}
      {PRIORITY_LABEL[priority]}
    </span>
  );
}
