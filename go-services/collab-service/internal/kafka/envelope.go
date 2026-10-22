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
	AssigneeID         *string `json:"assigneeId"`         // null = unassign → skip notify
	PreviousAssigneeID *string `json:"previousAssigneeId"`
}
