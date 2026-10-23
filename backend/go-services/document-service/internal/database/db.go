package database

import (
	"fmt"

	"github.com/collabflow/document-service/internal/model"
	"gorm.io/driver/postgres"
	"gorm.io/gorm"
	"gorm.io/gorm/logger"
)

func Connect(dsn string) (*gorm.DB, error) {
	db, err := gorm.Open(postgres.Open(dsn), &gorm.Config{
		Logger: logger.Default.LogMode(logger.Warn),
	})
	if err != nil {
		return nil, fmt.Errorf("open postgres: %w", err)
	}
	if err := db.AutoMigrate(&model.Document{}); err != nil {
		return nil, fmt.Errorf("auto-migrate documents: %w", err)
	}
	return db, nil
}
