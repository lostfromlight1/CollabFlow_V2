import { Icon } from "@/components/ui/icon";
import {
  STATUS_CHIP,
  STATUS_ICON,
  STATUS_LABEL,
  type TaskStatus,
} from "@/lib/task-meta";
import { cn } from "@/lib/utils";

export function StatusChip({
  status,
  label,
  className,
  showIcon = true,
}: {
  status: TaskStatus;
  label?: string;
  className?: string;
  showIcon?: boolean;
}) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1 rounded-md px-2 py-0.5 text-xs font-medium",
        STATUS_CHIP[status],
        className
      )}
    >
      {showIcon ? <Icon icon={STATUS_ICON[status]} size="sm" /> : null}
      {label || STATUS_LABEL[status]}
    </span>
  );
}
