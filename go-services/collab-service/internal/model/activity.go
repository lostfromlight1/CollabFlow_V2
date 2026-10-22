package model

import (
	"time"

	"github.com/google/uuid"
)

// ActivityFeed maps docs/database.md activity_feed (collab-owned).
type ActivityFeed struct {
	ID             uuid.UUID  `gorm:"type:uuid;primaryKey;default:gen_random_uuid()"`
	ActionType     string     `gorm:"size:50;not null"`
	EntityType     string     `gorm:"size:20;not null;index:idx_activity_feed_entity,priority:1"`
	EntityID       uuid.UUID  `gorm:"type:uuid;not null;index:idx_activity_feed_entity,priority:2"`
	OrganizationID uuid.UUID  `gorm:"type:uuid;not null;index:idx_activity_feed_organization_id"`
	WorkspaceID    *uuid.UUID `gorm:"type:uuid;index:idx_activity_feed_workspace_id"`
	ProjectID      *uuid.UUID `gorm:"type:uuid;index:idx_activity_feed_project_id"`
	UserID         uuid.UUID  `gorm:"type:uuid;not null;index:idx_activity_feed_user_id"`
	ActorName      string     `gorm:"size:255;not null"`
	ActorEmail     string     `gorm:"size:255;not null"`
	Changes        []byte     `gorm:"type:jsonb"` // optional before/after JSON
	Metadata       []byte     `gorm:"type:jsonb"` // extra context JSON
	CreatedAt      time.Time  `gorm:"not null;index:idx_activity_feed_created_at"`
}

func (ActivityFeed) TableName() string { return "activity_feed" }

// UserActor is a read of auth users for actor_name / actor_email / email stub.
type UserActor struct {
	ID    uuid.UUID `gorm:"type:uuid;primaryKey"`
	Name  string    `gorm:"size:255"`
	Email string    `gorm:"size:255"`
}

func (UserActor) TableName() string { return "users" }
