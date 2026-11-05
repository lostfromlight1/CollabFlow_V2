"use client";

import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { Dialog } from "@/components/ui/dialog";
import { FormField } from "@/components/forms/form-field";
import { InlineAlert } from "@/components/feedback/inline-alert";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

const schema = z.object({
  name: z.string().min(1).max(255),
  description: z.string().max(10000).optional(),
  priority: z.enum(["LOW", "MEDIUM", "HIGH", "CRITICAL"]),
  dueDate: z.string().optional(),
});

export type TaskFormValues = z.infer<typeof schema>;

const empty: TaskFormValues = { name: "", description: "", priority: "MEDIUM", dueDate: "" };

export function TaskFormDialog({
  open,
  title,
  submitLabel,
  formKey,
  initial,
  error,
  onClose,
  onSubmit,
}: {
  open: boolean;
  title: string;
  submitLabel: string;
  formKey: string;
  initial?: Partial<TaskFormValues> | null;
  error?: string | null;
  onClose: () => void;
  onSubmit: (values: TaskFormValues) => Promise<void>;
}) {
  const { register, handleSubmit, reset, formState } = useForm<TaskFormValues>({
    resolver: zodResolver(schema),
    defaultValues: empty,
  });

  useEffect(() => {
    if (!open) return;
    reset({
      name: initial?.name ?? "",
      description: initial?.description ?? "",
      priority: initial?.priority ?? "MEDIUM",
      dueDate: initial?.dueDate ?? "",
    });
    // formKey changes when a different task is opened. initial is read at that moment.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, formKey, reset]);

  return (
    <Dialog open={open} title={title} onClose={onClose}>
      <form
        className="space-y-3"
        onSubmit={handleSubmit(async (values) => {
          await onSubmit(values);
        })}
      >
        {error ? <InlineAlert>{error}</InlineAlert> : null}
        <FormField label="Name" htmlFor="task-name" error={formState.errors.name?.message}>
          <Input id="task-name" {...register("name")} />
        </FormField>
        <FormField label="Description" htmlFor="task-description">
          <textarea
            id="task-description"
            rows={4}
            {...register("description")}
            className="w-full rounded-lg border border-input bg-card/60 px-2.5 py-2 text-sm outline-none"
          />
        </FormField>
        <FormField label="Priority" htmlFor="task-priority">
          <select id="task-priority" className="h-8 w-full rounded-lg border border-input bg-background px-2 text-sm" {...register("priority")}>
            <option value="LOW">Low</option>
            <option value="MEDIUM">Medium</option>
            <option value="HIGH">High</option>
            <option value="CRITICAL">Urgent</option>
          </select>
        </FormField>
        <FormField label="Due date" htmlFor="task-due">
          <Input id="task-due" type="date" {...register("dueDate")} />
        </FormField>
        <div className="flex justify-end gap-2">
          <Button type="button" variant="outline" onClick={onClose}>Cancel</Button>
          <Button type="submit" disabled={formState.isSubmitting}>{submitLabel}</Button>
        </div>
      </form>
    </Dialog>
  );
}
