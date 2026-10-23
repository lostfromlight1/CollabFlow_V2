import { PageTransition } from "@/components/motion/page-transition";
import { cn } from "@/lib/utils";

export function AuthShell({
  children,
  className,
}: {
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <main
      className={cn(
        "relative flex flex-1 items-center justify-center overflow-hidden p-6",
        className
      )}
    >
      <div
        aria-hidden
        className="pointer-events-none absolute inset-0 bg-[radial-gradient(ellipse_at_top,_oklch(0.92_0.04_155)_0%,_transparent_55%),radial-gradient(ellipse_at_bottom_right,_oklch(0.94_0.03_145)_0%,_transparent_50%)]"
      />
      <PageTransition className="relative z-10 w-full max-w-md">{children}</PageTransition>
    </main>
  );
}
