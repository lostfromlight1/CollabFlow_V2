"use client";

import { useEffect, useRef } from "react";

export function Dialog({
  open,
  title,
  onClose,
  children,
}: {
  open: boolean;
  title: string;
  onClose: () => void;
  children: React.ReactNode;
}) {
  const ref = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    if (open && !el.open) el.showModal();
    if (!open && el.open) el.close();
  }, [open]);

  return (
    <dialog
      ref={ref}
      onClose={onClose}
      className="w-[min(32rem,calc(100%-2rem))] rounded-xl border border-border bg-card p-0 text-foreground shadow-lg backdrop:bg-black/40"
    >
      <div className="flex items-center justify-between border-b border-border px-4 py-3">
        <h2 className="font-heading text-lg font-semibold">{title}</h2>
        <button type="button" className="text-sm text-muted-foreground" onClick={onClose}>
          Close
        </button>
      </div>
      <div className="space-y-3 p-4">{children}</div>
    </dialog>
  );
}
