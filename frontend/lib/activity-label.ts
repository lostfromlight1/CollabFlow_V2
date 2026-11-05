import type { ActivityItem } from "@/lib/api/collab";

function kind(entityType: string): string {
  switch (entityType) {
    case "ORGANIZATION":
      return "organization";
    case "WORKSPACE":
      return "workspace";
    case "PROJECT":
      return "project";
    case "TASK":
      return "task";
    case "DOCUMENT":
      return "document";
    case "SPRINT":
      return "sprint";
    default:
      return entityType ? entityType.toLowerCase() : "item";
  }
}

function withName(phrase: string, name?: string): string {
  const trimmed = name?.trim();
  return trimmed ? `${phrase} ${trimmed}` : phrase;
}

/** Plain-language line for an activity row. Unknown codes stay unchanged. */
export function activitySentence(item: Pick<ActivityItem, "actionType" | "entityType" | "metadata">): string {
  const name = item.metadata?.name;
  const effect = item.metadata?.effect;
  const noun = kind(item.entityType);

  switch (item.actionType) {
    case "ORGANIZATION_CREATED":
    case "WORKSPACE_CREATED":
    case "PROJECT_CREATED":
    case "TASK_CREATED":
      return withName(`created the ${noun}`, name);
    case "ORGANIZATION_UPDATED":
    case "WORKSPACE_UPDATED":
    case "PROJECT_UPDATED":
      return withName(`updated the ${noun}`, name);
    case "ORGANIZATION_DELETED":
    case "WORKSPACE_DELETED":
      return withName(effect === "deactivated" ? `deactivated the ${noun}` : `deleted the ${noun}`, name);
    case "PROJECT_DELETED":
      return withName(effect === "archived" ? `archived the ${noun}` : `deleted the ${noun}`, name);
    case "TASK_DELETED":
      return withName("deleted the task", name);
    case "TASK_UPDATED":
      return name?.trim() ? `changed the status of ${name.trim()}` : "changed the task status";
    case "TASK_ASSIGNED":
      return withName("assigned the task", name);
    case "COMMENT_ADDED":
      return withName(`commented on the ${noun}`, name);
    case "MEMBER_JOINED":
      return withName(`added a member to the ${noun}`, name);
    case "MEMBER_LEFT":
      return withName(`removed a member from the ${noun}`, name);
    case "SPRINT_STARTED":
      return withName("started the sprint", name);
    case "DOCUMENT_UPLOADED":
      return withName("uploaded the document", name);
    default:
      return item.actionType;
  }
}
