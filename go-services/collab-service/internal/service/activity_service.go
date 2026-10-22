package service

import (
	"context"
	"encoding/json"
	"fmt"
	"log"

	"github.com/collabflow/collab-service/internal/kafka"
	"github.com/collabflow/collab-service/internal/model"
	"github.com/collabflow/collab-service/internal/repository"
	"github.com/google/uuid"
)

// ActivityService writes activity_feed from Kafka and serves list APIs.
type ActivityService struct {
	repo *repository.ActivityRepository
}

func NewActivityService(repo *repository.ActivityRepository) *ActivityService {
	return &ActivityService{repo: repo}
}

func activityProcessedID(eventID string) string {
	// Same Kafka eventId can also create a notification — use a distinct idempotency key.
	return eventID + ":activity"
}

func (s *ActivityService) HandleTaskAssigned(ctx context.Context, env kafka.Envelope, data kafka.TaskAssignedData) error {
	key := activityProcessedID(env.EventID)
	done, err := s.repo.AlreadyProcessed(ctx, key)
	if err != nil {
		return err
	}
	if done {
		return nil
	}
	if data.AssigneeID == nil || *data.AssigneeID == "" {
		return s.repo.MarkProcessed(ctx, key, "task.assigned")
	}

	taskID, err := uuid.Parse(data.TaskID)
	if err != nil {
		return fmt.Errorf("taskId: %w", err)
	}
	assigneeID, err := uuid.Parse(*data.AssigneeID)
	if err != nil {
		return fmt.Errorf("assigneeId: %w", err)
	}
	task, err := s.repo.FindTask(ctx, taskID)
	if err != nil {
		return err
	}

	// Prefer assigner from Kafka; fall back to reporter then assignee.
	actorID := task.ReporterID
	if data.AssignerID != nil && *data.AssignerID != "" {
		if id, err := uuid.Parse(*data.AssignerID); err == nil {
			actorID = id
		}
	}
	if actorID == uuid.Nil {
		actorID = assigneeID
	}
	actor, err := s.resolveActor(ctx, actorID)
	if err != nil {
		return err
	}

	projectID := task.ProjectID
	wsID := task.WorkspaceID
	meta, _ := json.Marshal(map[string]any{
		"assigneeId": assigneeID.String(),
		"taskName":   task.Name,
	})
	row := &model.ActivityFeed{
		ActionType:     "TASK_ASSIGNED",
		EntityType:     "TASK",
		EntityID:       taskID,
		OrganizationID: task.OrganizationID,
		WorkspaceID:    &wsID,
		ProjectID:      &projectID,
		UserID:         actor.ID,
		ActorName:      actor.Name,
		ActorEmail:     actor.Email,
		Metadata:       meta,
	}
	if err := s.repo.CreateWithProcessed(ctx, row, key, "task.assigned"); err != nil {
		return err
	}
	log.Printf("activity TASK_ASSIGNED eventId=%s task=%s", env.EventID, taskID)
	return nil
}

func (s *ActivityService) HandleTaskCreated(ctx context.Context, env kafka.Envelope, data kafka.TaskCreatedData) error {
	key := activityProcessedID(env.EventID)
	done, err := s.repo.AlreadyProcessed(ctx, key)
	if err != nil {
		return err
	}
	if done {
		return nil
	}

	taskID, err := uuid.Parse(data.TaskID)
	if err != nil {
		return fmt.Errorf("taskId: %w", err)
	}
	reporterID, err := uuid.Parse(data.ReporterID)
	if err != nil {
		return fmt.Errorf("reporterId: %w", err)
	}

	task, err := s.repo.FindTask(ctx, taskID)
	if err != nil {
		return err
	}
	actor, err := s.resolveActor(ctx, reporterID)
	if err != nil {
		return err
	}

	projectID := task.ProjectID
	wsID := task.WorkspaceID
	meta, _ := json.Marshal(map[string]any{"taskName": task.Name})
	row := &model.ActivityFeed{
		ActionType:     "TASK_CREATED",
		EntityType:     "TASK",
		EntityID:       taskID,
		OrganizationID: task.OrganizationID,
		WorkspaceID:    &wsID,
		ProjectID:      &projectID,
		UserID:         actor.ID,
		ActorName:      actor.Name,
		ActorEmail:     actor.Email,
		Metadata:       meta,
	}
	if err := s.repo.CreateWithProcessed(ctx, row, key, "task.created"); err != nil {
		return err
	}
	log.Printf("activity TASK_CREATED eventId=%s task=%s", env.EventID, taskID)
	return nil
}

func (s *ActivityService) HandleCommentAdded(ctx context.Context, env kafka.Envelope, data kafka.CommentAddedData) error {
	key := activityProcessedID(env.EventID)
	done, err := s.repo.AlreadyProcessed(ctx, key)
	if err != nil {
		return err
	}
	if done {
		return nil
	}

	entityID, err := uuid.Parse(data.EntityID)
	if err != nil {
		return fmt.Errorf("entityId: %w", err)
	}
	authorID, err := uuid.Parse(data.AuthorID)
	if err != nil {
		return fmt.Errorf("authorId: %w", err)
	}
	commentID, err := uuid.Parse(data.CommentID)
	if err != nil {
		return fmt.Errorf("commentId: %w", err)
	}

	actor, err := s.resolveActor(ctx, authorID)
	if err != nil {
		return err
	}

	entityType := data.EntityType
	orgID := uuid.Nil
	var projectID *uuid.UUID
	var workspaceID *uuid.UUID

	switch entityType {
	case "TASK":
		task, err := s.repo.FindTask(ctx, entityID)
		if err != nil {
			return err
		}
		orgID = task.OrganizationID
		pid, wid := task.ProjectID, task.WorkspaceID
		projectID, workspaceID = &pid, &wid
	case "PROJECT":
		oid, err := s.repo.OrgIDForProject(ctx, entityID)
		if err != nil {
			return err
		}
		orgID = oid
		projectID = &entityID
	default:
		return fmt.Errorf("unsupported entityType %s", entityType)
	}

	meta, _ := json.Marshal(map[string]any{"commentId": commentID.String()})
	row := &model.ActivityFeed{
		ActionType:     "COMMENT_ADDED",
		EntityType:     entityType,
		EntityID:       entityID,
		OrganizationID: orgID,
		WorkspaceID:    workspaceID,
		ProjectID:      projectID,
		UserID:         actor.ID,
		ActorName:      actor.Name,
		ActorEmail:     actor.Email,
		Metadata:       meta,
	}
	if err := s.repo.CreateWithProcessed(ctx, row, key, "comment.added"); err != nil {
		return err
	}
	log.Printf("activity COMMENT_ADDED eventId=%s entity=%s/%s", env.EventID, entityType, entityID)
	return nil
}

func (s *ActivityService) resolveActor(ctx context.Context, userID uuid.UUID) (*model.UserActor, error) {
	actor, err := s.repo.FindUser(ctx, userID)
	if err != nil {
		return nil, err // must be a real users row (FK on activity_feed.user_id)
	}
	if actor.Name == "" {
		actor.Name = actor.Email
	}
	return actor, nil
}

func (s *ActivityService) ListByEntity(
	ctx context.Context,
	entityType string,
	entityID uuid.UUID,
	orgID *uuid.UUID,
	page, size int,
) ([]model.ActivityFeed, int64, error) {
	return s.repo.ListByEntity(ctx, entityType, entityID, orgID, page, size)
}

func (s *ActivityService) ListByUser(
	ctx context.Context,
	userID uuid.UUID,
	orgID *uuid.UUID,
	page, size int,
) ([]model.ActivityFeed, int64, error) {
	return s.repo.ListByUser(ctx, userID, orgID, page, size)
}

func (s *ActivityService) ListByOrg(ctx context.Context, orgID uuid.UUID, page, size int) ([]model.ActivityFeed, int64, error) {
	return s.repo.ListByOrg(ctx, orgID, page, size)
}
