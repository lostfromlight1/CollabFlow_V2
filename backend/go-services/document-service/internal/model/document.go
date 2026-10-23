package model

import (
	"time"

	"github.com/google/uuid"
)

// Document matches docs/database.md documents (latest row per file name).
// GORM AutoMigrate creates the table the same way collab-service creates its tables.
type Document struct {
	ID               uuid.UUID  `gorm:"type:uuid;primaryKey"`
	Name             string     `gorm:"size:255;not null"`
	Description      *string    `gorm:"type:text"`
	FilePath         string     `gorm:"size:500;not null"`
	FileType         string     `gorm:"size:100;not null"`
	FileSize         int64      `gorm:"not null"`
	EntityType       string     `gorm:"size:20;not null;index:idx_documents_entity,priority:1"`
	EntityID         uuid.UUID  `gorm:"type:uuid;not null;index:idx_documents_entity,priority:2"`
	UploadedBy       uuid.UUID  `gorm:"type:uuid;not null;index"`
	Version          int        `gorm:"not null"`
	ParentDocumentID *uuid.UUID `gorm:"type:uuid"`
	IsLatest         bool       `gorm:"not null;index"`
	CreatedAt        time.Time  `gorm:"not null"`
	UpdatedAt        time.Time  `gorm:"not null"`
}

func (Document) TableName() string { return "documents" }
