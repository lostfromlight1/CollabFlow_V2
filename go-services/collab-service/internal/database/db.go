package database

import (
	"fmt"
	"log"

	"github.com/collabflow/collab-service/internal/model"
	"gorm.io/driver/postgres"
	"gorm.io/gorm"
	"gorm.io/gorm/logger"
)

// Connect opens Postgres and runs AutoMigrate for collab-owned tables.
func Connect(dsn string) (*gorm.DB, error) {
	db, err := gorm.Open(postgres.Open(dsn), &gorm.Config{
		Logger: logger.Default.LogMode(logger.Warn),
	})
	if err != nil {
		return nil, fmt.Errorf("open postgres: %w", err)
	}

	if err := db.AutoMigrate(
		&model.Notification{},
		&model.ProcessedEvent{},
		&model.Invitation{},
		&model.Comment{},
		&model.CommentMention{},
	); err != nil {
		return nil, fmt.Errorf("auto-migrate: %w", err)
	}

	log.Println("database connected; collab tables migrated")
	return db, nil
}
