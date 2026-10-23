import type { LucideIcon } from "lucide-react";
import { Label } from "@/components/ui/label";
import { Icon } from "@/components/ui/icon";
import { cn } from "@/lib/utils";

export function FormField({
  label,
  htmlFor,
  error,
  children,
  className,
  icon,
}: {
  label: string;
  htmlFor?: string;
  error?: string;
  children: React.ReactNode;
  className?: string;
  icon?: LucideIcon;
}) {
  return (
    <div className={cn("space-y-2", className)}>
      <Label htmlFor={htmlFor} className="inline-flex items-center gap-1.5">
        {icon ? <Icon icon={icon} size="md" className="text-primary/80" /> : null}
        {label}
      </Label>
      {children}
      {error ? <p className="text-sm text-destructive">{error}</p> : null}
    </div>
  );
}
