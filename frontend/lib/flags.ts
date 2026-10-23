"use client";

import { useEffect, useState } from "react";
import { listFeatureFlags, type FeatureFlag } from "@/lib/api/admin";

let flagCache: FeatureFlag[] | null = null;

/** A flag the server has never stored is enabled, so existing screens stay on. */
export function useFeatureFlag(key: string): boolean {
  const [enabled, setEnabled] = useState(true);

  useEffect(() => {
    const apply = (rows: FeatureFlag[]) => {
      const row = rows.find((item) => item.key === key);
      setEnabled(row ? row.enabled : true);
    };
    if (flagCache) {
      apply(flagCache);
      return;
    }
    listFeatureFlags()
      .then((rows) => {
        flagCache = rows ?? [];
        apply(flagCache);
      })
      .catch(() => setEnabled(true));
  }, [key]);

  return enabled;
}

export function clearFeatureFlagCache(): void {
  flagCache = null;
}
