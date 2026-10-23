"use client";

import { useState } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Suspense } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { AuthShell } from "@/components/layout/auth-shell";
import { FormField } from "@/components/forms/form-field";
import { InlineAlert } from "@/components/feedback/inline-alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { resetPassword } from "@/lib/api/auth";
import { ApiError } from "@/lib/api/client";

const schema = z.object({
  token: z.string().min(1, "Token is required"),
  newPassword: z.string().min(8, "At least 8 characters").max(100),
});

function ResetForm() {
  const params = useSearchParams();
  const [error, setError] = useState<string | null>(null);
  const [done, setDone] = useState(false);
  const { register, handleSubmit, formState } = useForm({
    resolver: zodResolver(schema),
    defaultValues: { token: params.get("token") ?? "", newPassword: "" },
  });

  const onSubmit = handleSubmit(async (values) => {
    setError(null);
    try {
      await resetPassword(values.token, values.newPassword);
      setDone(true);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Reset failed");
    }
  });

  return (
    <Card className="border-border/60 shadow-sm">
      <CardHeader>
        <CardTitle className="font-heading text-xl">Choose a new password</CardTitle>
        <CardDescription>Paste the token from the reset email if it is not already filled in.</CardDescription>
      </CardHeader>
      <CardContent>
        <form className="flex flex-col gap-4" onSubmit={onSubmit}>
          <FormField label="Token" htmlFor="token" error={formState.errors.token?.message}>
            <Input id="token" {...register("token")} />
          </FormField>
          <FormField label="New password" htmlFor="newPassword" error={formState.errors.newPassword?.message}>
            <Input id="newPassword" type="password" {...register("newPassword")} />
          </FormField>
          {done ? <InlineAlert variant="success">Password updated. You can sign in.</InlineAlert> : null}
          {error ? <InlineAlert>{error}</InlineAlert> : null}
          <Button type="submit" disabled={formState.isSubmitting}>Update password</Button>
          <Link href="/login" className="interactive-hover-link text-center text-sm text-muted-foreground">Sign in</Link>
        </form>
      </CardContent>
    </Card>
  );
}

export default function ResetPasswordPage() {
  return (
    <AuthShell>
      <Suspense fallback={<p className="text-sm text-muted-foreground">Loading…</p>}>
        <ResetForm />
      </Suspense>
    </AuthShell>
  );
}
