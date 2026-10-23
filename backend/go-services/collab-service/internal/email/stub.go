package email

import "log"

// Message is the structured outbound email shape used by Kafka email.send
// and by in-process callers (e.g. task-assigned notification).
// Wave 1: log only. Wave 8: swap StubSendMessage for real SMTP / email-service.
type Message struct {
	To            string
	Subject       string
	Body          string
	Template      string // verify_email | password_reset | invitation | generic
	CorrelationID string
}

// StubSend is the MVP helper for callers that only have to/subject/body.
func StubSend(to, subject, body string) {
	StubSendMessage(Message{
		To:       to,
		Subject:  subject,
		Body:     body,
		Template: "generic",
	})
}

// StubSendMessage logs structured fields — no SMTP yet.
// Why log template + correlationId: auth (and later invites) send them on
// email.send so we can prove the consumer path before wiring a real sender.
func StubSendMessage(m Message) {
	log.Printf(
		"EMAIL_STUB to=%s subject=%q template=%s correlationId=%s body=%q",
		m.To, m.Subject, m.Template, m.CorrelationID, m.Body,
	)
}
