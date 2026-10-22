package repository

import (
	"context"
	"time"

	"github.com/collabflow/collab-service/internal/model"
	"github.com/google/uuid"
	"gorm.io/gorm"
)

type NotificationRepository struct {
	db *gorm.DB
}

func NewNotificationRepository(db *gorm.DB) *NotificationRepository {
	return &NotificationRepository{db: db}
}

func (r *NotificationRepository) Create(ctx context.Context, n *model.Notification) error {
	return r.db.WithContext(ctx).Create(n).Error
}

func (r *NotificationRepository) FindTask(ctx context.Context, taskID uuid.UUID) (*model.TaskRef, error) {
	var t model.TaskRef
	err := r.db.WithContext(ctx).First(&t, "id = ?", taskID).Error
	if err != nil {
		return nil, err
	}
	return &t, nil
}

func (r *NotificationRepository) FindUser(ctx context.Context, userID uuid.UUID) (*model.UserActor, error) {
	var u model.UserActor
	err := r.db.WithContext(ctx).First(&u, "id = ?", userID).Error
	if err != nil {
		return nil, err
	}
	return &u, nil
}

func (r *NotificationRepository) AlreadyProcessed(ctx context.Context, eventID string) (bool, error) {
	var count int64
	err := r.db.WithContext(ctx).Model(&model.ProcessedEvent{}).
		Where("event_id = ?", eventID).Count(&count).Error
	return count > 0, err
}

func (r *NotificationRepository) MarkProcessed(ctx context.Context, eventID, topic string) error {
	return r.db.WithContext(ctx).Create(&model.ProcessedEvent{
		EventID:     eventID,
		Topic:       topic,
		ProcessedAt: time.Now().UTC(),
	}).Error
}

// CreateWithProcessed inserts notification + processed_events in one transaction.
func (r *NotificationRepository) CreateWithProcessed(
	ctx context.Context,
	n *model.Notification,
	eventID, topic string,
) error {
	return r.db.WithContext(ctx).Transaction(func(tx *gorm.DB) error {
		var count int64
		if err := tx.Model(&model.ProcessedEvent{}).Where("event_id = ?", eventID).Count(&count).Error; err != nil {
			return err
		}
		if count > 0 {
			return nil // already done — idempotent success
		}
		if err := tx.Create(n).Error; err != nil {
			return err
		}
		return tx.Create(&model.ProcessedEvent{
			EventID:     eventID,
			Topic:       topic,
			ProcessedAt: time.Now().UTC(),
		}).Error
	})
}

func (r *NotificationRepository) ListByRecipient(
	ctx context.Context,
	recipientID uuid.UUID,
	orgID *uuid.UUID,
	page, size int,
) ([]model.Notification, int64, error) {
	q := r.db.WithContext(ctx).Model(&model.Notification{}).Where("recipient_id = ?", recipientID)
	if orgID != nil {
		q = q.Where("organization_id = ?", *orgID)
	}
	var total int64
	if err := q.Count(&total).Error; err != nil {
		return nil, 0, err
	}
	var list []model.Notification
	err := q.Order("created_at DESC").Offset(page * size).Limit(size).Find(&list).Error
	return list, total, err
}

func (r *NotificationRepository) CountUnread(ctx context.Context, recipientID uuid.UUID, orgID *uuid.UUID) (int64, error) {
	q := r.db.WithContext(ctx).Model(&model.Notification{}).
		Where("recipient_id = ? AND is_read = ?", recipientID, false)
	if orgID != nil {
		q = q.Where("organization_id = ?", *orgID)
	}
	var n int64
	err := q.Count(&n).Error
	return n, err
}

func (r *NotificationRepository) MarkRead(ctx context.Context, id, recipientID uuid.UUID) error {
	now := time.Now().UTC()
	res := r.db.WithContext(ctx).Model(&model.Notification{}).
		Where("id = ? AND recipient_id = ?", id, recipientID).
		Updates(map[string]any{"is_read": true, "read_at": now})
	if res.Error != nil {
		return res.Error
	}
	if res.RowsAffected == 0 {
		return gorm.ErrRecordNotFound
	}
	return nil
}

func (r *NotificationRepository) MarkAllRead(ctx context.Context, recipientID uuid.UUID, orgID *uuid.UUID) (int64, error) {
	now := time.Now().UTC()
	q := r.db.WithContext(ctx).Model(&model.Notification{}).
		Where("recipient_id = ? AND is_read = ?", recipientID, false)
	if orgID != nil {
		q = q.Where("organization_id = ?", *orgID)
	}
	res := q.Updates(map[string]any{"is_read": true, "read_at": now})
	return res.RowsAffected, res.Error
}
