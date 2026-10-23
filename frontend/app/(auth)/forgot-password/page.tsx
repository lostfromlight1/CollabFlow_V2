"use client";

import { useState } from "react";
import Link from "next/link";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { AuthShell } from "@/components/layout/auth-shell";
import { FormField } from "@/components/forms/form-field";
import { InlineAlert } from "@/components/feedback/inline-alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { forgotPassword } from "@/lib/api/auth";
import { ApiError } from "@/lib/api/client";

const schema = z.object({ email: z.string().email("Enter a valid email") });

export default function ForgotPasswordPage() {
  const [error, setError] = useState<string | null>(null);
  const [done, setDone] = useState(false);
  const { register, handleSubmit, formState } = useForm({
    resolver: zodResolver(schema),
    defaultValues: { email: "" },
  });

  const onSubmit = handleSubmit(async (values) => {
    setError(null);
    try {
      await forgotPassword(values.email);
      setDone(true);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Request failed");
    }
  });

  return (
    <AuthShell>
      <Card className="border-border/60 shadow-sm">
        <CardHeader>
          <CardTitle className="font-heading text-xl">Reset password</CardTitle>
          <CardDescription>We email a reset link when the account exists.</CardDescription>
        </CardHeader>
        <CardContent>
          <form className="flex flex-col gap-4" onSubmit={onSubmit}>
            <FormField label="Email" htmlFor="email" error={formState.errors.email?.message}>
              <Input id="email" type="email" {...register("email")} />
            </FormField>
            {done ? <InlineAlert variant="success">If the account exists, a reset message was sent.</InlineAlert> : null}
            {error ? <InlineAlert>{error}</InlineAlert> : null}
            <Button type="submit" disabled={formState.isSubmitting}>Send reset link</Button>
            <Link href="/login" className="interactive-hover-link text-center text-sm text-muted-foreground">Back to sign in</Link>
          </form>
        </CardContent>
      </Card>
    </AuthShell>
  );
}
