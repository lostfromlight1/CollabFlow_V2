package model

import (
	"time"

	"github.com/google/uuid"
)

// Notification maps to table notifications (docs/database.md).
// GORM tags ≈ JPA @Column / @Table. We do not declare FK constraints here —
// users/organizations already exist from Java Flyway (shared DB MVP).
type Notification struct {
	ID               uuid.UUID  `gorm:"type:uuid;primaryKey;default:gen_random_uuid()"`
	RecipientID      uuid.UUID  `gorm:"type:uuid;not null;index:idx_notifications_recipient_id"`
	SenderID         *uuid.UUID `gorm:"type:uuid"` // nil = system
	NotificationType string     `gorm:"size:50;not null"`
	Title            string     `gorm:"size:255;not null"`
	Message          string     `gorm:"type:text;not null"`
	EntityType       string     `gorm:"size:20;not null"`
	EntityID         uuid.UUID  `gorm:"type:uuid;not null"`
	OrganizationID   uuid.UUID  `gorm:"type:uuid;not null;index:idx_notifications_organization_id"`
	IsRead           bool       `gorm:"not null;default:false;index:idx_notifications_is_read"`
	ReadAt           *time.Time
	ActionURL        *string    `gorm:"size:500"`
	CreatedAt        time.Time  `gorm:"not null;index:idx_notifications_created_at,sort:desc"`
}

func (Notification) TableName() string {
	return "notifications"
}
