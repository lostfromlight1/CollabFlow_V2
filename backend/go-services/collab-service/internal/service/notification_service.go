package service

import (
	"context"
	"fmt"
	"log"

	"github.com/collabflow/collab-service/internal/email"
	"github.com/collabflow/collab-service/internal/kafka"
	"github.com/collabflow/collab-service/internal/model"
	"github.com/collabflow/collab-service/internal/repository"
	"github.com/google/uuid"
	"gorm.io/gorm"
)

const topicTaskAssigned = "task.assigned"

type NotificationService struct {
	repo *repository.NotificationRepository
}

func NewNotificationService(repo *repository.NotificationRepository) *NotificationService {
	return &NotificationService{repo: repo}
}

// HandleTaskAssigned turns a Kafka event into a notifications row.
// Java analogy: @KafkaListener method that writes to the DB.
func (s *NotificationService) HandleTaskAssigned(ctx context.Context, env kafka.Envelope, data kafka.TaskAssignedData) error {
	done, err := s.repo.AlreadyProcessed(ctx, env.EventID)
	if err != nil {
		return err
	}
	if done {
		log.Printf("task.assigned eventId=%s already processed", env.EventID)
		return nil
	}

	if data.AssigneeID == nil || *data.AssigneeID == "" {
		log.Printf("task.assigned eventId=%s skipped (unassign)", env.EventID)
		return s.repo.MarkProcessed(ctx, env.EventID, topicTaskAssigned)
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
		if err == gorm.ErrRecordNotFound {
			return fmt.Errorf("task %s not found", taskID)
		}
		return err
	}

	var senderID *uuid.UUID
	if data.AssignerID != nil && *data.AssignerID != "" {
		if id, err := uuid.Parse(*data.AssignerID); err == nil {
			senderID = &id
		}
	}

	actionURL := fmt.Sprintf("/projects/%s/tasks/%s", data.ProjectID, data.TaskID)
	n := &model.Notification{
		RecipientID:      assigneeID,
		SenderID:         senderID,
		NotificationType: "TASK_ASSIGNED",
		Title:            "Task assigned to you",
		Message:          fmt.Sprintf("You were assigned to task: %s", task.Name),
		EntityType:       "TASK",
		EntityID:         taskID,
		OrganizationID:   task.OrganizationID,
		IsRead:           false,
		ActionURL:        &actionURL,
	}

	if err := s.repo.CreateWithProcessed(ctx, n, env.EventID, topicTaskAssigned); err != nil {
		return err
	}
	log.Printf("notification created eventId=%s recipient=%s task=%s", env.EventID, assigneeID, taskID)

	// Week 11: no SMTP yet — log as if an email were sent.
	if u, err := s.repo.FindUser(ctx, assigneeID); err == nil && u.Email != "" {
		email.StubSend(u.Email, n.Title, n.Message)
	}
	return nil
}

// HandleStatusChanged notifies the assignee when someone else moves the task.
// Idempotency key is separate from the activity row for the same Kafka event.
// This path does not call the email stub. Assignment mail stays on task.assigned.
func (s *NotificationService) HandleStatusChanged(ctx context.Context, env kafka.Envelope, data kafka.ScopeEventData) error {
	key := env.EventID + ":notification"
	done, err := s.repo.AlreadyProcessed(ctx, key)
	if err != nil {
		return err
	}
	if done {
		return nil
	}
	if data.ActorID == "" || data.TaskID == "" {
		log.Printf("task.status_changed eventId=%s notification skipped (missing actor or task)", env.EventID)
		return s.repo.MarkProcessed(ctx, key, "task.status_changed")
	}
	taskID, err := uuid.Parse(data.TaskID)
	if err != nil {
		return fmt.Errorf("taskId: %w", err)
	}
	actorID, err := uuid.Parse(data.ActorID)
	if err != nil {
		return fmt.Errorf("actorId: %w", err)
	}
	task, err := s.repo.FindTask(ctx, taskID)
	if err != nil {
		return err
	}
	if task.AssigneeID == nil || *task.AssigneeID == actorID {
		return s.repo.MarkProcessed(ctx, key, "task.status_changed")
	}
	orgID := task.OrganizationID
	if data.OrganizationID != "" {
		if parsed, err := uuid.Parse(data.OrganizationID); err == nil {
			orgID = parsed
		}
	}
	name := task.Name
	if data.Name != "" {
		name = data.Name
	}
	actionURL := fmt.Sprintf("/projects/%s/tasks/%s", data.ProjectID, data.TaskID)
	n := &model.Notification{
		RecipientID:      *task.AssigneeID,
		SenderID:         &actorID,
		NotificationType: "TASK_UPDATED",
		Title:            "Task status changed",
		Message:          fmt.Sprintf("Status changed on task: %s", name),
		EntityType:       "TASK",
		EntityID:         taskID,
		OrganizationID:   orgID,
		IsRead:           false,
		ActionURL:        &actionURL,
	}
	if err := s.repo.CreateWithProcessed(ctx, n, key, "task.status_changed"); err != nil {
		return err
	}
	log.Printf("status notification eventId=%s recipient=%s task=%s", env.EventID, *task.AssigneeID, taskID)
	return nil
}

func (s *NotificationService) ListMine(
	ctx context.Context,
	userID uuid.UUID,
	orgID *uuid.UUID,
	page, size int,
) ([]model.Notification, int64, error) {
	return s.repo.ListByRecipient(ctx, userID, orgID, page, size)
}

func (s *NotificationService) UnreadCount(ctx context.Context, userID uuid.UUID, orgID *uuid.UUID) (int64, error) {
	return s.repo.CountUnread(ctx, userID, orgID)
}

func (s *NotificationService) MarkRead(ctx context.Context, userID, notificationID uuid.UUID) error {
	return s.repo.MarkRead(ctx, notificationID, userID)
}

func (s *NotificationService) MarkAllRead(ctx context.Context, userID uuid.UUID, orgID *uuid.UUID) (int64, error) {
	return s.repo.MarkAllRead(ctx, userID, orgID)
}
