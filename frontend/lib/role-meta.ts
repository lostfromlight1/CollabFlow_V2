/**
 * Org role labels + chip classes (CSS tokens in globals.css).
 */
import { Crown, Shield, User, type LucideIcon } from "lucide-react";

export type OrgRole = "Owner" | "Admin" | "Member";

export const ROLE_CHIP: Record<OrgRole, string> = {
  Owner: "chip-role-owner",
  Admin: "chip-role-admin",
  Member: "chip-role-member",
};

export const ROLE_ICON: Record<OrgRole, LucideIcon> = {
  Owner: Crown,
  Admin: Shield,
  Member: User,
};
