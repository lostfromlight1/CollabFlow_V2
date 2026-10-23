import type { UserProfile } from "@/lib/types/api";

const USERS_KEY = "collabflow.scratchUsers";
const ORGS_KEY = "collabflow.scratchOrgs";

export type ScratchOrg = {
  id: string;
  name: string;
  slug: string;
};

function readJson<T>(key: string): T[] {
  if (typeof window === "undefined") return [];
  try {
    const raw = sessionStorage.getItem(key);
    if (!raw) return [];
    return JSON.parse(raw) as T[];
  } catch {
    return [];
  }
}

function writeJson<T>(key: string, value: T[]) {
  sessionStorage.setItem(key, JSON.stringify(value));
}

export function pushScratchUser(user: UserProfile) {
  const list = readJson<UserProfile>(USERS_KEY).filter((u) => u.id !== user.id);
  list.unshift(user);
  writeJson(USERS_KEY, list.slice(0, 50));
  sessionStorage.setItem("collabflow.lastCreatedUserId", user.id);
}

export function getScratchUsers(): UserProfile[] {
  return readJson<UserProfile>(USERS_KEY);
}

export function pushScratchOrg(org: ScratchOrg) {
  const list = readJson<ScratchOrg>(ORGS_KEY).filter((o) => o.id !== org.id);
  list.unshift(org);
  writeJson(ORGS_KEY, list.slice(0, 50));
  sessionStorage.setItem("collabflow.lastCreatedOrgId", org.id);
}

export function getScratchOrgs(): ScratchOrg[] {
  return readJson<ScratchOrg>(ORGS_KEY);
}
