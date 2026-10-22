package model

import (
	"github.com/google/uuid"
)

// TaskRef is a read-only projection of Java-owned table tasks (shared DB).
// Used to resolve organization_id / reporter when handling task events.
type TaskRef struct {
	ID             uuid.UUID `gorm:"type:uuid;primaryKey"`
	Name           string
	ProjectID      uuid.UUID `gorm:"column:project_id;type:uuid"`
	WorkspaceID    uuid.UUID `gorm:"column:workspace_id;type:uuid"`
	OrganizationID uuid.UUID `gorm:"column:organization_id;type:uuid"`
	ReporterID     uuid.UUID `gorm:"column:reporter_id;type:uuid"`
}

func (TaskRef) TableName() string {
	return "tasks"
}
