package kafka

import (
	"encoding/json"
	"time"
)

// Envelope matches Java DomainEvent (docs/microservice.md).
// Field names are camelCase because Jackson serializes Java records that way.
// Canonical body key is "data"; "payload" is accepted for flexibility.
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

// TaskAssignedData matches TaskAssignedPayload from task-service.
type TaskAssignedData struct {
	TaskID             string  `json:"taskId"`
	ProjectID          string  `json:"projectId"`
	WorkspaceID        string  `json:"workspaceId"`
	OrganizationID     string  `json:"organizationId"`
	AssigneeID         *string `json:"assigneeId"`         // null = unassign → skip notify
	PreviousAssigneeID *string `json:"previousAssigneeId"`
	AssignerID         *string `json:"assignerId"`
}

// TaskCreatedData matches TaskCreatedPayload from task-service.
type TaskCreatedData struct {
	TaskID      string `json:"taskId"`
	ProjectID   string `json:"projectId"`
	WorkspaceID string `json:"workspaceId"`
	Name        string `json:"name"`
	ReporterID  string `json:"reporterId"`
	StatusID    string `json:"statusId"`
}

// CommentAddedData matches collab-service producer payload on comment.added.
type CommentAddedData struct {
	CommentID  string `json:"commentId"`
	EntityType string `json:"entityType"`
	EntityID   string `json:"entityId"`
	AuthorID   string `json:"authorId"`
}

// EmailSendData matches auth EmailSendPayload (Java DomainEvent.data).
type EmailSendData struct {
	To            string `json:"to"`
	Subject       string `json:"subject"`
	Body          string `json:"body"`
	Template      string `json:"template"` // verify_email | password_reset | invitation | generic
	CorrelationID string `json:"correlationId"`
}
