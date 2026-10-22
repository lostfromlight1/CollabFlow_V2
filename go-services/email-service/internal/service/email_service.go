package service

import (
	"context"
	"log"

	"github.com/collabflow/email-service/internal/config"
	"github.com/collabflow/email-service/internal/kafka"
	"github.com/collabflow/email-service/internal/smtp"
)

// EmailService consumes email.send and delivers via real SMTP.
type EmailService struct {
	cfg config.Config
}

func NewEmailService(cfg config.Config) *EmailService {
	return &EmailService{cfg: cfg}
}

// HandleEmailSend is the Kafka handler (≈ @KafkaListener).
func (s *EmailService) HandleEmailSend(ctx context.Context, env kafka.Envelope, data kafka.EmailSendData) error {
	_ = ctx
	out := smtp.Outbound{
		To:            data.To,
		Subject:       data.Subject,
		Body:          data.Body,
		Template:      data.Template,
		CorrelationID: data.CorrelationID,
		EventID:       env.EventID,
	}
	smtp.LogReady(s.cfg, out)
	if err := smtp.Send(s.cfg, out); err != nil {
		log.Printf("email.send SMTP failed eventId=%s to=%s: %v", env.EventID, data.To, err)
		return err
	}
	log.Printf("email.send delivered eventId=%s to=%s template=%s", env.EventID, data.To, data.Template)
	return nil
}
