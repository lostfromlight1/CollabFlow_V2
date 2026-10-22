package model

import (
	"time"
)

// ProcessedEvent stores Kafka eventId values we have already handled.
// Why: Kafka may redeliver; checking this table makes the consumer idempotent.
type ProcessedEvent struct {
	EventID     string    `gorm:"size:64;primaryKey"`
	Topic       string    `gorm:"size:100;not null"`
	ProcessedAt time.Time `gorm:"not null"`
}

func (ProcessedEvent) TableName() string {
	return "processed_events"
}
