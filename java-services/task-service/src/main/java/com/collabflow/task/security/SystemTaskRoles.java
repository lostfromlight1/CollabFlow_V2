package com.collabflow.task.security;

import java.util.UUID;

/** Stable IDs from task Flyway seed + platform PROJECT roles. */
public final class SystemTaskRoles {

    public static final UUID DEFAULT_WORKFLOW = UUID.fromString("c0000000-0000-4000-8000-000000000001");

    public static final UUID STATUS_BACKLOG = UUID.fromString("c0000000-0000-4000-8000-000000000011");
    public static final UUID STATUS_TODO = UUID.fromString("c0000000-0000-4000-8000-000000000012");
    public static final UUID STATUS_IN_PROGRESS = UUID.fromString("c0000000-0000-4000-8000-000000000013");
    public static final UUID STATUS_IN_REVIEW = UUID.fromString("c0000000-0000-4000-8000-000000000014");
    public static final UUID STATUS_DONE = UUID.fromString("c0000000-0000-4000-8000-000000000015");

    public static final UUID TASK_TYPE_TASK = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    public static final UUID TASK_TYPE_BUG = UUID.fromString("d0000000-0000-4000-8000-000000000002");
    public static final UUID TASK_TYPE_STORY = UUID.fromString("d0000000-0000-4000-8000-000000000003");
    public static final UUID TASK_TYPE_EPIC = UUID.fromString("d0000000-0000-4000-8000-000000000004");

    public static final String TASK_CREATE = "task.create";
    public static final String TASK_READ = "task.read";
    public static final String TASK_UPDATE = "task.update";
    public static final String TASK_DELETE = "task.delete";
    public static final String TASK_ASSIGN = "task.assign";
    public static final String TASK_MOVE = "task.move";

    private SystemTaskRoles() {}
}
