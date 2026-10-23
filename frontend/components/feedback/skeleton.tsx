"use client";

import { useEffect, useState } from "react";
import { cn } from "@/lib/utils";
import { motionDuration } from "@/lib/motion/tokens";

/** Block placeholder; parent DelayedSkeletonGate controls flash avoidance. */
export function Skeleton({ className }: { className?: string }) {
  return (
    <div
      className={cn("rounded-md bg-muted/80", className)}
      aria-hidden
    />
  );
}

/**
 * Shows children only after skeletonDelayMs of `loading`.
 * If loading ends sooner, renders nothing (no flash) until content is ready.
 */
export function DelayedSkeletonGate({
  loading,
  skeleton,
  children,
}: {
  loading: boolean;
  skeleton: React.ReactNode;
  children: React.ReactNode;
}) {
  const [showSkeleton, setShowSkeleton] = useState(false);

  useEffect(() => {
    if (!loading) {
      setShowSkeleton(false);
      return;
    }
    const id = window.setTimeout(() => {
      setShowSkeleton(true);
    }, motionDuration.skeletonDelayMs);
    return () => window.clearTimeout(id);
  }, [loading]);

  if (!loading) return <>{children}</>;
  if (!showSkeleton) return null;
  return <>{skeleton}</>;
}

export function ShellSkeleton() {
  return (
    <div className="flex min-h-[50vh] w-full gap-4 p-4">
      <Skeleton className="hidden h-64 w-52 shrink-0 md:block" />
      <div className="flex flex-1 flex-col gap-3">
        <Skeleton className="h-10 w-full" />
        <Skeleton className="h-32 w-full" />
        <Skeleton className="h-24 w-3/4" />
      </div>
    </div>
  );
}

export function FormCardSkeleton() {
  return (
    <div className="mx-auto w-full max-w-md space-y-4 rounded-xl border border-border/60 bg-card p-6">
      <Skeleton className="h-6 w-40" />
      <Skeleton className="h-4 w-full" />
      <Skeleton className="h-9 w-full" />
      <Skeleton className="h-9 w-full" />
      <Skeleton className="h-9 w-28" />
    </div>
  );
}

export function ListSkeleton({ rows = 3 }: { rows?: number }) {
  return (
    <div className="flex flex-col gap-2">
      {Array.from({ length: rows }).map((_, i) => (
        <Skeleton key={i} className="h-11 w-full" />
      ))}
    </div>
  );
}

export function DashboardSkeleton() {
  return (
    <div className="space-y-4">
      <Skeleton className="h-8 w-48" />
      <div className="grid gap-4 sm:grid-cols-2">
        <Skeleton className="h-28 w-full" />
        <Skeleton className="h-28 w-full" />
      </div>
    </div>
  );
}
