package kafka

import (
	"encoding/json"
	"time"
)

// Envelope matches Java DomainEvent / collab-service kafka.Envelope.
// Same camelCase JSON so auth-produced email.send works without adapters.
type Envelope struct {
	EventID   string          `json:"eventId"`
	EventType string          `json:"eventType"`
	Timestamp time.Time       `json:"timestamp"`
	Version   string          `json:"version"`
	Data      json.RawMessage `json:"data"`
	Payload   json.RawMessage `json:"payload"`
}

// Body returns the event payload — prefer data (DomainEvent), else payload.
func (e Envelope) Body() json.RawMessage {
	if len(e.Data) > 0 && string(e.Data) != "null" {
		return e.Data
	}
	return e.Payload
}

// EmailSendData matches auth EmailSendPayload and collab EmailSendData.
type EmailSendData struct {
	To            string `json:"to"`
	Subject       string `json:"subject"`
	Body          string `json:"body"`
	Template      string `json:"template"` // verify_email | password_reset | invitation | generic
	CorrelationID string `json:"correlationId"`
}
