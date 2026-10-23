/**
 * Real-route lucide icons only.
 * Product nav icons.
 */
import {
  Activity,
  Bell,
  Building2,
  FolderKanban,
  Flag,
  Home,
  LayoutDashboard,
  LayoutGrid,
  LogIn,
  Mail,
  Megaphone,
  ScrollText,
  Settings,
  Sparkles,
  Users,
} from "lucide-react";

export const Icons = {
  home: Home,
  admin: LayoutDashboard,
  users: Users,
  organizations: Building2,
  workspaces: LayoutGrid,
  projects: FolderKanban,
  login: LogIn,
  mail: Mail,
  sparkles: Sparkles,
  activity: Activity,
  notifications: Bell,
  settings: Settings,
  audit: ScrollText,
  announcements: Megaphone,
  flags: Flag,
} as const;
