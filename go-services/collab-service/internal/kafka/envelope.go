package kafka

import (
	"encoding/json"
	"time"
)

// Envelope matches Java DomainEvent (docs/microservice.md).
// field names are camelCase because Jackson serializes Java records that way.
type Envelope struct {
	EventID   string          `json:"eventId"`
	EventType string          `json:"eventType"`
	Timestamp time.Time       `json:"timestamp"`
	Version   string          `json:"version"`
	Data      json.RawMessage `json:"data"`
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
