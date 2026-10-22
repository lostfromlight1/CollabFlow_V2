package service

import (
	"context"
	"fmt"
	"log"

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

	actionURL := fmt.Sprintf("/projects/%s/tasks/%s", data.ProjectID, data.TaskID)
	n := &model.Notification{
		RecipientID:      assigneeID,
		SenderID:         nil, // assign actor not in payload yet
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
	return nil
}

func (s *NotificationService) ListMine(ctx context.Context, userID uuid.UUID) ([]model.Notification, error) {
	return s.repo.ListByRecipient(ctx, userID)
}

func (s *NotificationService) MarkRead(ctx context.Context, userID, notificationID uuid.UUID) error {
	return s.repo.MarkRead(ctx, notificationID, userID)
}

func (s *NotificationService) MarkAllRead(ctx context.Context, userID uuid.UUID) (int64, error) {
	return s.repo.MarkAllRead(ctx, userID)
}
