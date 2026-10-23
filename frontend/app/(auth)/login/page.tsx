"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { KeyRound, LogIn, Mail } from "lucide-react";
import { AuthShell } from "@/components/layout/auth-shell";
import { FormField } from "@/components/forms/form-field";
import { InlineAlert } from "@/components/feedback/inline-alert";
import { Button } from "@/components/ui/button";
import { Icon } from "@/components/ui/icon";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { getMyProfile, login, resendVerification } from "@/lib/api/auth";
import { ApiError } from "@/lib/api/client";
import { persistAuth, postAuthPath } from "@/lib/auth/routing";

const schema = z.object({
  email: z.string().min(1, "Email is required").email("Enter a valid email"),
  password: z.string().min(1, "Password is required"),
});

type FormValues = z.infer<typeof schema>;

export default function LoginPage() {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    getValues,
    formState: { isSubmitting, errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { email: "", password: "" },
  });

  const onSubmit = handleSubmit(
    async (values) => {
      setError(null);
      try {
        const auth = await login(values.email, values.password);
        persistAuth(auth, {
          id: auth.userId,
          email: auth.email,
          name: auth.name,
          masterAdmin: false,
        });
        const profile = await getMyProfile();
        const session = persistAuth(auth, {
          id: profile.id,
          email: profile.email,
          name: profile.name,
          masterAdmin: profile.masterAdmin,
        });
        router.replace(postAuthPath(session.user, auth));
      } catch (e) {
        setError(e instanceof ApiError ? e.message : "Login failed");
      }
    },
    () => setError("Check email and password")
  );

  return (
    <AuthShell>
      <Card className="border-border/60 shadow-sm">
        <CardHeader>
          <CardTitle className="font-heading text-xl">Sign in</CardTitle>
          <CardDescription>
            Master admin opens the portal. Members pick an organization next.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form className="flex flex-col gap-4" onSubmit={onSubmit}>
            <FormField
              label="Email"
              htmlFor="email"
              error={errors.email?.message}
              icon={Mail}
            >
              <Input id="email" type="email" autoComplete="username" {...register("email")} />
            </FormField>
            <FormField
              label="Password"
              htmlFor="password"
              error={errors.password?.message}
              icon={KeyRound}
            >
              <Input
                id="password"
                type="password"
                autoComplete="current-password"
                {...register("password")}
              />
            </FormField>
            {error ? <InlineAlert>{error}</InlineAlert> : null}
            {info ? <InlineAlert variant="success">{info}</InlineAlert> : null}
            <Button type="submit" disabled={isSubmitting} className="w-full">
              <Icon icon={LogIn} size="md" />
              {isSubmitting ? "Signing in…" : "Sign in"}
            </Button>
            <button
              type="button"
              className="interactive-hover-link text-sm text-muted-foreground"
              onClick={() => {
                const email = getValues("email");
                setError(null);
                setInfo(null);
                if (!email) {
                  setError("Enter your email, then resend verification");
                  return;
                }
                resendVerification(email)
                  .then(() => setInfo("If the account is unverified, a message was sent"))
                  .catch((e) => setError(e instanceof ApiError ? e.message : "Resend failed"));
              }}
            >
              Resend verification email
            </button>
            <div className="flex flex-col gap-1 text-center text-sm">
              <Link href="/forgot-password" className="interactive-hover-link text-muted-foreground">
                Forgot password
              </Link>
              <Link href="/register" className="interactive-hover-link text-muted-foreground">
                Create an account
              </Link>
              <Link href="/" className="interactive-hover-link text-muted-foreground">
                Back to home
              </Link>
            </div>
          </form>
        </CardContent>
      </Card>
    </AuthShell>
  );
}
