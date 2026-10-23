"use client";

import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { logout } from "@/lib/api/auth";
import { clearSession, getSession } from "@/lib/auth/session";

export function LogoutButton() {
  const router = useRouter();

  return (
    <Button
      type="button"
      variant="outline"
      size="sm"
      onClick={async () => {
        const s = getSession();
        if (s?.refreshToken) {
          await logout(s.refreshToken);
        }
        clearSession();
        router.replace("/");
      }}
    >
      Log out
    </Button>
  );
}
