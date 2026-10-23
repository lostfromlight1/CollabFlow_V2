"use client";

import { Suspense, useState } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { AuthShell } from "@/components/layout/auth-shell";
import { InlineAlert } from "@/components/feedback/inline-alert";
import { FormCardSkeleton } from "@/components/feedback/skeleton";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { apiFetch, ApiError } from "@/lib/api/client";

function VerifyInner() {
  const params = useSearchParams();
  const token = params.get("token") || "";
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function verify() {
    if (!token) {
      setError("Missing token in URL (?token=)");
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await apiFetch<unknown>("/api/v1/auth/verify-email", {
        method: "POST",
        auth: false,
        body: { token },
      });
      setMessage("Email verified. You can sign in.");
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Verify failed");
    } finally {
      setBusy(false);
    }
  }

  return (
    <Card className="border-border/60 shadow-sm">
      <CardHeader>
        <CardTitle className="font-heading text-xl">Verify email</CardTitle>
        <CardDescription>
          For public register flows. Admin-created users are already verified.
        </CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-3">
        {token ? (
          <p className="break-all text-xs text-muted-foreground">
            Token: {token.slice(0, 24)}…
          </p>
        ) : (
          <InlineAlert variant="info">No token in query string.</InlineAlert>
        )}
        {error ? <InlineAlert>{error}</InlineAlert> : null}
        {message ? <InlineAlert variant="success">{message}</InlineAlert> : null}
        <Button type="button" disabled={busy || !token} onClick={verify}>
          {busy ? "Verifying…" : "Verify"}
        </Button>
        <Link href="/login" className="interactive-hover-link text-sm">
          Back to login
        </Link>
      </CardContent>
    </Card>
  );
}

export default function VerifyEmailPage() {
  return (
    <AuthShell>
      <Suspense fallback={<FormCardSkeleton />}>
        <VerifyInner />
      </Suspense>
    </AuthShell>
  );
}
