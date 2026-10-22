package smtp

import (
	"log"

	"github.com/collabflow/email-service/internal/config"
)

// Outbound is the message shape from Kafka email.send data (+ event id).
type Outbound struct {
	To            string
	Subject       string
	Body          string
	Template      string
	CorrelationID string
	EventID       string
}

// LogReady logs what we are about to send (useful when debugging MailHog / providers).
func LogReady(cfg config.Config, m Outbound) {
	log.Printf(
		"EMAIL_SMTP_SEND eventId=%s to=%s from=%s subject=%q template=%s correlationId=%s "+
			"smtpHost=%s smtpPort=%s smtpTLS=%v smtpUserSet=%v",
		m.EventID,
		m.To,
		cfg.SMTPFrom,
		m.Subject,
		m.Template,
		m.CorrelationID,
		cfg.SMTPHost,
		cfg.SMTPPort,
		cfg.SMTPTLS,
		cfg.SMTPUser != "",
	)
}
