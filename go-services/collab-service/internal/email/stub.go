package email

import "log"

// StubSend logs an outbound email instead of calling SMTP/SendGrid.
// Week 11 MVP: no separate email-service; swap this for a real sender later.
func StubSend(to, subject, body string) {
	log.Printf("EMAIL_STUB to=%s subject=%q body=%q", to, subject, body)
}
