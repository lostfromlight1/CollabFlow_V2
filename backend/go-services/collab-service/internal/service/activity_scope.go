package service

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"strings"

	"github.com/collabflow/collab-service/internal/kafka"
	"github.com/collabflow/collab-service/internal/model"
	"github.com/google/uuid"
)

// activitySpec maps a Kafka topic to an activity_feed action and entity type.
// Java analogy: a switch in one @KafkaListener that shares the same write path.
func activitySpec(topic string) (action, entityType string, ok bool) {
	switch topic {
	case "organization.created":
		return "ORGANIZATION_CREATED", "ORGANIZATION", true
	case "organization.updated":
		return "ORGANIZATION_UPDATED", "ORGANIZATION", true
	case "organization.deleted":
		return "ORGANIZATION_DELETED", "ORGANIZATION", true
	case "organization.member_added":
		return "MEMBER_JOINED", "ORGANIZATION", true
	case "organization.member_removed":
		return "MEMBER_LEFT", "ORGANIZATION", true
	case "workspace.created":
		return "WORKSPACE_CREATED", "WORKSPACE", true
	case "workspace.updated":
		return "WORKSPACE_UPDATED", "WORKSPACE", true
	case "workspace.deleted":
		return "WORKSPACE_DELETED", "WORKSPACE", true
	case "workspace.member_added":
		return "MEMBER_JOINED", "WORKSPACE", true
	case "workspace.member_removed":
		return "MEMBER_LEFT", "WORKSPACE", true
	case "project.created":
		return "PROJECT_CREATED", "PROJECT", true
	case "project.updated":
		return "PROJECT_UPDATED", "PROJECT", true
	case "project.deleted":
		return "PROJECT_DELETED", "PROJECT", true
	case "project.member_added":
		return "MEMBER_JOINED", "PROJECT", true
	case "project.member_removed":
		return "MEMBER_LEFT", "PROJECT", true
	case "task.deleted":
		return "TASK_DELETED", "TASK", true
	case "task.status_changed":
		return "TASK_UPDATED", "TASK", true
	default:
		return "", "", false
	}
}

// HandleScoped writes one activity row from a platform or task side-effect event.
// task.deleted must use the payload only: the task row is already gone.
func (s *ActivityService) HandleScoped(ctx context.Context, topic string, env kafka.Envelope, data kafka.ScopeEventData) error {
	key := activityProcessedID(env.EventID)
	done, err := s.repo.AlreadyProcessed(ctx, key)
	if err != nil {
		return err
	}
	if done {
		return nil
	}

	action, entityType, ok := activitySpec(topic)
	if !ok {
		return fmt.Errorf("no activity mapping for topic %s", topic)
	}
	if data.ActorID == "" {
		log.Printf("activity skip topic=%s eventId=%s missing actorId", topic, env.EventID)
		return s.repo.MarkProcessed(ctx, key, topic)
	}
	actorID, err := uuid.Parse(data.ActorID)
	if err != nil {
		return fmt.Errorf("actorId: %w", err)
	}
	actor, err := s.resolveActor(ctx, actorID)
	if err != nil {
		return err
	}

	orgID, entityID, workspaceID, projectID, err := scopeIDs(topic, data)
	if err != nil {
		return err
	}

	meta := map[string]any{}
	if data.Name != "" {
		meta["name"] = data.Name
	}
	if data.UserID != "" {
		meta["userId"] = data.UserID
	}
	if data.Slug != "" {
		meta["slug"] = data.Slug
	}
	switch topic {
	case "organization.deleted", "workspace.deleted":
		meta["effect"] = "deactivated"
	case "project.deleted":
		meta["effect"] = "archived"
	}
	var metaJSON []byte
	if len(meta) > 0 {
		metaJSON, _ = json.Marshal(meta)
	}

	var changes []byte
	if topic == "task.status_changed" {
		changes, _ = json.Marshal(map[string]any{
			"statusId": map[string]string{"old": data.FromStatusID, "new": data.ToStatusID},
		})
	}

	row := &model.ActivityFeed{
		ActionType:     action,
		EntityType:     entityType,
		EntityID:       entityID,
		OrganizationID: orgID,
		WorkspaceID:    workspaceID,
		ProjectID:      projectID,
		UserID:         actor.ID,
		ActorName:      actor.Name,
		ActorEmail:     actor.Email,
		Changes:        changes,
		Metadata:       metaJSON,
	}
	if err := s.repo.CreateWithProcessed(ctx, row, key, topic); err != nil {
		return err
	}
	log.Printf("activity %s eventId=%s topic=%s", action, env.EventID, topic)
	return nil
}

func scopeIDs(topic string, data kafka.ScopeEventData) (orgID, entityID uuid.UUID, workspaceID, projectID *uuid.UUID, err error) {
	orgID, err = parseRequired(data.OrganizationID, "organizationId")
	if err != nil {
		return uuid.Nil, uuid.Nil, nil, nil, err
	}
	switch {
	case topic == "task.deleted" || topic == "task.status_changed":
		entityID, err = parseRequired(data.TaskID, "taskId")
		if err != nil {
			return uuid.Nil, uuid.Nil, nil, nil, err
		}
		ws, err := parseRequired(data.WorkspaceID, "workspaceId")
		if err != nil {
			return uuid.Nil, uuid.Nil, nil, nil, err
		}
		prj, err := parseRequired(data.ProjectID, "projectId")
		if err != nil {
			return uuid.Nil, uuid.Nil, nil, nil, err
		}
		return orgID, entityID, &ws, &prj, nil
	case strings.HasPrefix(topic, "organization."):
		return orgID, orgID, nil, nil, nil
	case strings.HasPrefix(topic, "workspace."):
		entityID, err = parseRequired(data.WorkspaceID, "workspaceId")
		if err != nil {
			return uuid.Nil, uuid.Nil, nil, nil, err
		}
		return orgID, entityID, &entityID, nil, nil
	case strings.HasPrefix(topic, "project."):
		ws, err := parseRequired(data.WorkspaceID, "workspaceId")
		if err != nil {
			return uuid.Nil, uuid.Nil, nil, nil, err
		}
		entityID, err = parseRequired(data.ProjectID, "projectId")
		if err != nil {
			return uuid.Nil, uuid.Nil, nil, nil, err
		}
		return orgID, entityID, &ws, &entityID, nil
	default:
		return uuid.Nil, uuid.Nil, nil, nil, fmt.Errorf("unsupported topic %s", topic)
	}
}

func parseRequired(raw, field string) (uuid.UUID, error) {
	if raw == "" {
		return uuid.Nil, fmt.Errorf("%s is required", field)
	}
	id, err := uuid.Parse(raw)
	if err != nil {
		return uuid.Nil, fmt.Errorf("%s: %w", field, err)
	}
	return id, nil
}
