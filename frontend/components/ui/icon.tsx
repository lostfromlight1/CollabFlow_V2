import type { LucideIcon, LucideProps } from "lucide-react";
import { cn } from "@/lib/utils";

export type IconSize = "sm" | "md" | "lg" | "xl";

const sizeClass: Record<IconSize, string> = {
  sm: "size-3.5",
  md: "size-4",
  lg: "size-5",
  xl: "size-10",
};

export function Icon({
  icon: Lucide,
  size = "md",
  className,
  strokeWidth = 2,
  decorative = true,
  ...props
}: {
  icon: LucideIcon;
  size?: IconSize;
  decorative?: boolean;
} & Omit<LucideProps, "ref" | "size">) {
  return (
    <Lucide
      className={cn("shrink-0", sizeClass[size], className)}
      strokeWidth={strokeWidth}
      aria-hidden={decorative ? true : undefined}
      {...props}
    />
  );
}
