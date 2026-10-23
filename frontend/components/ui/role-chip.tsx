import { Icon } from "@/components/ui/icon";
import { ROLE_ICON, type OrgRole } from "@/lib/role-meta";
import { cn } from "@/lib/utils";

/** Explicit CSS vars so Tailwind can't drop role colors. */
const ROLE_STYLE: Record<OrgRole, string> = {
  Owner: "bg-[var(--role-owner)] text-[var(--role-owner-foreground)]",
  Admin: "bg-[var(--role-admin)] text-[var(--role-admin-foreground)]",
  Member: "bg-[var(--role-member)] text-[var(--role-member-foreground)]",
};

export function roleTone(name: string): OrgRole {
  const n = name.toLowerCase();
  if (n.includes("owner")) return "Owner";
  if (n.includes("admin")) return "Admin";
  return "Member";
}

export function RoleChip({
  role,
  label,
  className,
  showIcon = true,
}: {
  role: OrgRole | string;
  label?: string;
  className?: string;
  showIcon?: boolean;
}) {
  const tone = role === "Owner" || role === "Admin" || role === "Member" ? role : roleTone(role);
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1 rounded-md px-2 py-0.5 text-xs font-medium",
        ROLE_STYLE[tone],
        className
      )}
    >
      {showIcon ? <Icon icon={ROLE_ICON[tone]} size="sm" /> : null}
      {label || role}
    </span>
  );
}
