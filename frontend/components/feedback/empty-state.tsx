import type { LucideIcon } from "lucide-react";
import { Icon } from "@/components/ui/icon";
import { cn } from "@/lib/utils";

export function EmptyState({
  title,
  description,
  className,
  action,
  icon,
}: {
  title: string;
  description?: string;
  className?: string;
  action?: React.ReactNode;
  icon?: LucideIcon;
}) {
  return (
    <div
      className={cn(
        "flex flex-col items-start gap-3 rounded-xl border border-dashed border-border/80 bg-muted/30 px-4 py-6",
        className
      )}
    >
      {icon ? (
        <span className="text-primary/80">
          <Icon icon={icon} size="xl" />
        </span>
      ) : null}
      <p className="font-medium text-foreground">{title}</p>
      {description ? (
        <p className="text-sm text-muted-foreground">{description}</p>
      ) : null}
      {action}
    </div>
  );
}
