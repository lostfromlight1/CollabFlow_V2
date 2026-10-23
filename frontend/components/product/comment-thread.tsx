"use client";

import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { MessageSquare } from "lucide-react";
import { FormField } from "@/components/forms/form-field";
import { InlineAlert } from "@/components/feedback/inline-alert";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { ApiError } from "@/lib/api/client";
import {
  createComment,
  deleteComment,
  listComments,
  updateComment,
  type Comment,
} from "@/lib/api/collab";
import { getSession } from "@/lib/auth/session";

const schema = z.object({ content: z.string().min(1, "Write a comment").max(5000) });

export function CommentThread({
  entityType,
  entityId,
  canWrite,
}: {
  entityType: string;
  entityId: string;
  canWrite: boolean;
}) {
  const [rows, setRows] = useState<Comment[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [editing, setEditing] = useState<string | null>(null);
  const [draft, setDraft] = useState("");
  const me = getSession()?.user.id;
  const { register, handleSubmit, reset, formState } = useForm<{ content: string }>({
    resolver: zodResolver(schema),
    defaultValues: { content: "" },
  });

  async function load() {
    try {
      setRows(await listComments(entityType, entityId));
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Could not load comments");
    }
  }

  useEffect(() => {
    void load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [entityType, entityId]);

  const onSubmit = handleSubmit(async (values) => {
    setError(null);
    try {
      await createComment({ content: values.content, entityType, entityId });
      reset();
      await load();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Comment failed");
    }
  });

  return (
    <section className="space-y-3">
      <h2 className="flex items-center gap-2 text-sm font-medium">
        <MessageSquare className="size-4 text-primary" />
        Comments
      </h2>
      {error ? <InlineAlert>{error}</InlineAlert> : null}
      <ul className="space-y-2">
        {rows.map((row) => (
          <li key={row.id} className="rounded-xl border border-border/60 bg-card p-3 text-sm">
            {editing === row.id ? (
              <div className="flex gap-2">
                <Input value={draft} onChange={(e) => setDraft(e.target.value)} />
                <Button
                  type="button"
                  size="sm"
                  onClick={() => {
                    void updateComment(row.id, draft)
                      .then(load)
                      .then(() => setEditing(null))
                      .catch((e) => setError(e instanceof ApiError ? e.message : "Update failed"));
                  }}
                >
                  Save
                </Button>
              </div>
            ) : (
              <p>{row.content}</p>
            )}
            <p className="mt-1 text-xs text-muted-foreground">
              {row.authorId === me ? "You" : row.authorId.slice(0, 8)} · {row.createdAt}
            </p>
            {row.authorId === me ? (
              <div className="mt-2 flex gap-2">
                <Button
                  type="button"
                  size="sm"
                  variant="outline"
                  onClick={() => {
                    setEditing(row.id);
                    setDraft(row.content);
                  }}
                >
                  Edit
                </Button>
                <Button
                  type="button"
                  size="sm"
                  variant="destructive"
                  onClick={() => {
                    void deleteComment(row.id)
                      .then(load)
                      .catch((e) => setError(e instanceof ApiError ? e.message : "Delete failed"));
                  }}
                >
                  Delete
                </Button>
              </div>
            ) : null}
          </li>
        ))}
      </ul>
      {canWrite ? (
        <form className="flex flex-col gap-2" onSubmit={onSubmit}>
          <FormField label="New comment" htmlFor="content" error={formState.errors.content?.message}>
            <Input id="content" {...register("content")} />
          </FormField>
          <Button type="submit" disabled={formState.isSubmitting} className="self-start">
            Post comment
          </Button>
        </form>
      ) : (
        <p className="text-sm text-muted-foreground">Viewers can read comments.</p>
      )}
    </section>
  );
}