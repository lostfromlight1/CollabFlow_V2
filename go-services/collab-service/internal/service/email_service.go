package service

import (
	"context"
	"log"

	"github.com/collabflow/collab-service/internal/email"
	"github.com/collabflow/collab-service/internal/kafka"
	"github.com/collabflow/collab-service/internal/repository"
)

const topicEmailSend = "email.send"

// EmailService consumes email.send and routes to the stub (later: SMTP / email-service).
// Java analogy: @KafkaListener that only performs a side effect — no domain row.
type EmailService struct {
	// Reuses notification repo helpers for processed_events (same table).
	events *repository.NotificationRepository
}

func NewEmailService(events *repository.NotificationRepository) *EmailService {
	return &EmailService{events: events}
}

// HandleEmailSend logs via stub and marks the event processed (idempotent).
func (s *EmailService) HandleEmailSend(ctx context.Context, env kafka.Envelope, data kafka.EmailSendData) error {
	done, err := s.events.AlreadyProcessed(ctx, env.EventID)
	if err != nil {
		return err
	}
	if done {
		log.Printf("email.send eventId=%s already processed", env.EventID)
		return nil
	}

	email.StubSendMessage(email.Message{
		To:            data.To,
		Subject:       data.Subject,
		Body:          data.Body,
		Template:      data.Template,
		CorrelationID: data.CorrelationID,
	})

	if err := s.events.MarkProcessed(ctx, env.EventID, topicEmailSend); err != nil {
		return err
	}
	log.Printf("email.send handled eventId=%s to=%s template=%s", env.EventID, data.To, data.Template)
	return nil
}
