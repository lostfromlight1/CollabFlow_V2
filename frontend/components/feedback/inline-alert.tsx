import { cn } from "@/lib/utils";

export function InlineAlert({
  children,
  variant = "error",
  className,
}: {
  children: React.ReactNode;
  variant?: "error" | "success" | "info";
  className?: string;
}) {
  return (
    <p
      role="status"
      className={cn(
        "text-sm",
        variant === "error" && "text-destructive",
        variant === "success" && "text-primary",
        variant === "info" && "text-muted-foreground",
        className
      )}
    >
      {children}
    </p>
  );
}
