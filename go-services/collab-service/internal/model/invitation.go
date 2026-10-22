package model

import (
	"time"

	"github.com/google/uuid"
)

const (
	InviteTypeOrg       = "ORGANIZATION"
	InviteTypeWorkspace = "WORKSPACE"
	InviteTypeProject   = "PROJECT"

	InvitePending  = "PENDING"
	InviteAccepted = "ACCEPTED"
	InviteDeclined = "DECLINED"
	InviteExpired  = "EXPIRED"
)

// Invitation maps docs/database.md invitations table.
type Invitation struct {
	ID             uuid.UUID  `gorm:"type:uuid;primaryKey;default:gen_random_uuid()"`
	Email          string     `gorm:"size:255;not null;index:idx_invitations_email"`
	OrganizationID uuid.UUID  `gorm:"type:uuid;not null;index:idx_invitations_organization_id"`
	WorkspaceID    *uuid.UUID `gorm:"type:uuid;index:idx_invitations_workspace_id"`
	ProjectID      *uuid.UUID `gorm:"type:uuid"`
	InvitedBy      uuid.UUID  `gorm:"type:uuid;not null"`
	InvitationType string     `gorm:"size:20;not null;default:ORGANIZATION"`
	Status         string     `gorm:"size:20;not null;default:PENDING;index:idx_invitations_status"`
	Token          string     `gorm:"size:255;uniqueIndex:idx_invitations_token;not null"`
	ExpiresAt      time.Time  `gorm:"not null"`
	CreatedAt      time.Time  `gorm:"not null"`
}

func (Invitation) TableName() string { return "invitations" }

// UserRef reads auth users for email matching on accept.
type UserRef struct {
	ID    uuid.UUID `gorm:"type:uuid;primaryKey"`
	Email string    `gorm:"size:255"`
}

func (UserRef) TableName() string { return "users" }
