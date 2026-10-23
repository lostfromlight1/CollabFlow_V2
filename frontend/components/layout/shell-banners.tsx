"use client";

import { useEffect, useState } from "react";
import { endImpersonation, getActiveAnnouncement } from "@/lib/api/admin";
import { endImpersonationRestore, getSession } from "@/lib/auth/session";

export function ShellBanners() {
  const [announcement, setAnnouncement] = useState<{ title: string; body: string } | null>(null);
  const [impersonating, setImpersonating] = useState(false);
  const [target, setTarget] = useState("");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const session = getSession();
    if (!session) return;
    setImpersonating(Boolean(session.impersonating));
    setTarget(session.user.name || session.user.email);
    getActiveAnnouncement()
      .then((row) => setAnnouncement(row))
      .catch(() => setAnnouncement(null));
  }, []);

  async function exit() {
    setError(null);
    const targetId = getSession()?.user.id;
    try {
      await endImpersonation();
      endImpersonationRestore();
      window.location.assign(targetId ? `/admin/users/${targetId}` : "/admin/users");
    } catch (e) {
      setError(e instanceof Error ? e.message : "Could not end impersonation");
    }
  }

  if (!announcement && !impersonating) return null;

  return (
    <div className="mb-4 space-y-2">
      {impersonating ? (
        <div className="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-border bg-secondary px-3 py-2 text-sm">
          <span>Viewing as {target}. Changes are blocked.</span>
          <button type="button" className="font-medium text-primary" onClick={exit}>
            Exit
          </button>
        </div>
      ) : null}
      {announcement ? (
        <div className="rounded-lg border border-border bg-primary/10 px-3 py-2 text-sm">
          <p className="font-medium">{announcement.title}</p>
          <p className="text-muted-foreground">{announcement.body}</p>
        </div>
      ) : null}
      {error ? <p className="text-sm text-destructive">{error}</p> : null}
    </div>
  );
}
