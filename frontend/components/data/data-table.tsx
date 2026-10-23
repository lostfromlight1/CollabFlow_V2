"use client";

import type { ReactNode } from "react";
import type { LucideIcon } from "lucide-react";
import { EmptyState } from "@/components/feedback/empty-state";
import { Pagination } from "@/components/data/pagination";
import { SoftScroll } from "@/components/ui/soft-scroll";
import { cn } from "@/lib/utils";

export type DataColumn<T> = {
  id: string;
  header: ReactNode;
  cell: (row: T) => ReactNode;
  className?: string;
};

export function DataTable<T>({
  columns,
  rows,
  page,
  pageSize,
  total,
  onPageChange,
  emptyTitle = "No rows yet",
  emptyDescription,
  emptyIcon,
  getRowKey,
}: {
  columns: DataColumn<T>[];
  rows: T[];
  page: number;
  pageSize: number;
  total: number;
  onPageChange: (page: number) => void;
  emptyTitle?: string;
  emptyDescription?: string;
  emptyIcon?: LucideIcon;
  getRowKey: (row: T) => string;
}) {
  if (total === 0) {
    return (
      <EmptyState
        title={emptyTitle}
        description={emptyDescription}
        icon={emptyIcon}
      />
    );
  }

  return (
    <div className="space-y-2">
      <SoftScroll
        orientation="horizontal"
        className="rounded-xl border border-border/70 bg-card"
      >
        <table className="w-full min-w-[28rem] text-left text-sm">
          <thead className="border-b border-border/70 bg-muted/40">
            <tr>
              {columns.map((col) => (
                <th
                  key={col.id}
                  className={cn(
                    "px-3 py-2.5 font-medium text-muted-foreground",
                    col.className
                  )}
                >
                  {col.header}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr
                key={getRowKey(row)}
                className="interactive-hover-row border-b border-border/50 last:border-0"
              >
                {columns.map((col) => (
                  <td key={col.id} className={cn("px-3 py-2.5", col.className)}>
                    {col.cell(row)}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </SoftScroll>
      <Pagination
        page={page}
        pageSize={pageSize}
        total={total}
        onPageChange={onPageChange}
      />
    </div>
  );
}
