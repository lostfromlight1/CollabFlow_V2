"use client";

import { useEffect, useState } from "react";
import { endImpersonation, getActiveAnnouncement } from "@/lib/api/admin";
import { errorText } from "@/lib/api/client";
import { endImpersonationRestore, getSession } from "@/lib/auth/session";
import { Icons } from "@/lib/icons";

type BannerCopy = { id: string; title: string; body: string };

function dismissKey(userId: string) {
  return `collabflow.dismissedAnnouncement.${userId}`;
}

function bannerCopy(
  row: { id?: string; title?: string; body?: string } | null,
  userId: string,
): BannerCopy | null {
  if (!row?.id) return null;
  const title = row.title?.trim() ?? "";
  const body = row.body?.trim() ?? "";
  if (!title && !body) return null;
  if (typeof window !== "undefined" && localStorage.getItem(dismissKey(userId)) === row.id) {
    return null;
  }
  return { id: row.id, title, body };
}

export function ShellBanners() {
  const [announcement, setAnnouncement] = useState<BannerCopy | null>(null);
  const [impersonating, setImpersonating] = useState(false);
  const [target, setTarget] = useState("");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const session = getSession();
    if (!session) return;
    setImpersonating(Boolean(session.impersonating));
    setTarget(session.user.name || session.user.email);
    getActiveAnnouncement()
      .then((row) => setAnnouncement(bannerCopy(row, session.user.id)))
      .catch(() => setAnnouncement(null));
  }, []);

  function dismiss() {
    const session = getSession();
    if (!session || !announcement) return;
    localStorage.setItem(dismissKey(session.user.id), announcement.id);
    setAnnouncement(null);
  }

  async function exit() {
    setError(null);
    const targetId = getSession()?.user.id;
    try {
      await endImpersonation();
      endImpersonationRestore();
      window.location.assign(targetId ? `/admin/users/${targetId}` : "/admin/users");
    } catch (e) {
      setError(errorText(e, "Could not end impersonation"));
    }
  }

  if (!announcement && !impersonating) return null;

  const Megaphone = Icons.announcements;

  return (
    <div className="mb-4 space-y-2">
      {impersonating ? (
        <div className="flex flex-wrap items-center justify-between gap-2 rounded-xl border border-border bg-secondary px-4 py-2.5 text-sm">
          <span>Viewing as {target}. Changes are blocked.</span>
          <button type="button" className="font-medium text-primary" onClick={exit}>
            Exit
          </button>
        </div>
      ) : null}
      {announcement ? (
        <div className="flex gap-3 rounded-xl border border-primary/20 bg-primary/10 px-4 py-3">
          <span className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-primary/15 text-primary">
            <Megaphone className="size-4" />
          </span>
          <div className="min-w-0 flex-1">
            {announcement.title ? <p className="font-medium leading-snug">{announcement.title}</p> : null}
            {announcement.body ? (
              <p className={`text-sm leading-relaxed text-muted-foreground ${announcement.title ? "mt-1" : ""}`}>
                {announcement.body}
              </p>
            ) : null}
          </div>
          <button
            type="button"
            aria-label="Dismiss announcement"
            className="shrink-0 rounded-md px-1.5 text-lg leading-none text-muted-foreground hover:bg-primary/15 hover:text-foreground"
            onClick={dismiss}
          >
            ×
          </button>
        </div>
      ) : null}
      {error ? <p className="text-sm text-destructive">{error}</p> : null}
    </div>
  );
}
