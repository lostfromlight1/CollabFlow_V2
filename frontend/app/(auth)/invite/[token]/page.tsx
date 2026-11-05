"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { AuthShell } from "@/components/layout/auth-shell";
import { InlineAlert } from "@/components/feedback/inline-alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { acceptInvitation, peekInvitation, type Invitation } from "@/lib/api/collab";
import { errorText } from "@/lib/api/client";
import { getSession } from "@/lib/auth/session";

export default function InvitePage() {
  const params = useParams<{ token: string }>();
  const router = useRouter();
  const [invite, setInvite] = useState<Invitation | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);

  useEffect(() => {
    peekInvitation(params.token)
      .then(setInvite)
      .catch((e) => setError(errorText(e, "Invite not found")));
  }, [params.token]);

  const returnTo = `/invite/${params.token}`;
  const loginHref = `/login?next=${encodeURIComponent(returnTo)}`;
  const registerHref = invite
    ? `/register?email=${encodeURIComponent(invite.email)}&next=${encodeURIComponent(returnTo)}`
    : `/register?next=${encodeURIComponent(returnTo)}`;

  return (
    <AuthShell>
      <Card className="border-border/60 shadow-sm">
        <CardHeader>
          <CardTitle className="font-heading text-xl">Invitation</CardTitle>
          <CardDescription>
            {invite ? `${invite.email} · ${invite.status}` : "Loading invite…"}
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-3">
          {error ? <InlineAlert>{error}</InlineAlert> : null}
          {info ? <InlineAlert variant="success">{info}</InlineAlert> : null}
          {invite ? (
            <Button
              type="button"
              onClick={() => {
                if (!getSession()) {
                  router.push(loginHref);
                  return;
                }
                void acceptInvitation(params.token)
                  .then(() => {
                    setInfo("Accepted. Pick your organization if needed.");
                    router.push("/select-org");
                  })
                  .catch((e) => setError(errorText(e, "Accept failed")));
              }}
            >
              Accept invitation
            </Button>
          ) : null}
          <Link href={loginHref} className="interactive-hover-link block text-sm text-muted-foreground">
            Sign in first
          </Link>
          <Link href={registerHref} className="interactive-hover-link block text-sm text-muted-foreground">
            Create an account with this email
          </Link>
        </CardContent>
      </Card>
    </AuthShell>
  );
}
