"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { AuthShell } from "@/components/layout/auth-shell";
import { FormField } from "@/components/forms/form-field";
import { InlineAlert } from "@/components/feedback/inline-alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { getMyProfile, register as registerAccount } from "@/lib/api/auth";
import { ApiError } from "@/lib/api/client";
import { persistAuth, postAuthPath } from "@/lib/auth/routing";

const schema = z.object({
  name: z.string().min(1, "Name is required").max(255),
  email: z.string().email("Enter a valid email"),
  password: z.string().min(8, "At least 8 characters").max(100),
});

export default function RegisterPage() {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const { register, handleSubmit, formState } = useForm({
    resolver: zodResolver(schema),
    defaultValues: { name: "", email: "", password: "" },
  });

  const onSubmit = handleSubmit(
    async (values) => {
      setError(null);
      try {
        const auth = await registerAccount(values);
        persistAuth(auth, {
          id: auth.userId,
          email: auth.email,
          name: auth.name,
          masterAdmin: false,
        });
        const profile = await getMyProfile().catch(() => null);
        const session = persistAuth(auth, {
          id: profile?.id ?? auth.userId,
          email: profile?.email ?? auth.email,
          name: profile?.name ?? auth.name,
          masterAdmin: profile?.masterAdmin ?? false,
        });
        router.replace(postAuthPath(session.user, auth));
      } catch (e) {
        setError(e instanceof ApiError ? e.message : "Registration failed");
      }
    },
    () => setError("Check the form")
  );

  return (
    <AuthShell>
      <Card className="border-border/60 shadow-sm">
        <CardHeader>
          <CardTitle className="font-heading text-xl">Create account</CardTitle>
          <CardDescription>
            Public registration follows the server gate. If it is off, ask a master admin to create your user.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form className="flex flex-col gap-4" onSubmit={onSubmit}>
            <FormField label="Name" htmlFor="name" error={formState.errors.name?.message}>
              <Input id="name" {...register("name")} />
            </FormField>
            <FormField label="Email" htmlFor="email" error={formState.errors.email?.message}>
              <Input id="email" type="email" {...register("email")} />
            </FormField>
            <FormField label="Password" htmlFor="password" error={formState.errors.password?.message}>
              <Input id="password" type="password" {...register("password")} />
            </FormField>
            {error ? <InlineAlert>{error}</InlineAlert> : null}
            <Button type="submit" disabled={formState.isSubmitting}>
              {formState.isSubmitting ? "Creating…" : "Create account"}
            </Button>
            <Link href="/login" className="interactive-hover-link text-center text-sm text-muted-foreground">
              Already have an account
            </Link>
          </form>
        </CardContent>
      </Card>
    </AuthShell>
  );
}
