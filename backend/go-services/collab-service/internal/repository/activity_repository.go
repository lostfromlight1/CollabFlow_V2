package repository

import (
	"context"
	"time"

	"github.com/collabflow/collab-service/internal/model"
	"github.com/google/uuid"
	"gorm.io/gorm"
)

type ActivityRepository struct {
	db *gorm.DB
}

func NewActivityRepository(db *gorm.DB) *ActivityRepository {
	return &ActivityRepository{db: db}
}

func (r *ActivityRepository) FindTask(ctx context.Context, taskID uuid.UUID) (*model.TaskRef, error) {
	var t model.TaskRef
	err := r.db.WithContext(ctx).First(&t, "id = ?", taskID).Error
	if err != nil {
		return nil, err
	}
	return &t, nil
}

func (r *ActivityRepository) FindUser(ctx context.Context, userID uuid.UUID) (*model.UserActor, error) {
	var u model.UserActor
	err := r.db.WithContext(ctx).First(&u, "id = ?", userID).Error
	if err != nil {
		return nil, err
	}
	return &u, nil
}

func (r *ActivityRepository) OrgIDForProject(ctx context.Context, projectID uuid.UUID) (uuid.UUID, error) {
	var orgID uuid.UUID
	err := r.db.WithContext(ctx).Raw(`
		SELECT w.organization_id
		FROM projects p
		JOIN workspaces w ON w.id = p.workspace_id
		WHERE p.id = ?
	`, projectID).Scan(&orgID).Error
	if err != nil {
		return uuid.Nil, err
	}
	if orgID == uuid.Nil {
		return uuid.Nil, gorm.ErrRecordNotFound
	}
	return orgID, nil
}

func (r *ActivityRepository) AlreadyProcessed(ctx context.Context, eventID string) (bool, error) {
	var count int64
	err := r.db.WithContext(ctx).Model(&model.ProcessedEvent{}).
		Where("event_id = ?", eventID).Count(&count).Error
	return count > 0, err
}

func (r *ActivityRepository) MarkProcessed(ctx context.Context, eventID, topic string) error {
	return r.db.WithContext(ctx).Create(&model.ProcessedEvent{
		EventID:     eventID,
		Topic:       topic,
		ProcessedAt: time.Now().UTC(),
	}).Error
}

// CreateWithProcessed inserts activity + processed_events (idempotent).
// Use a distinct eventID key (e.g. rawEventID+":activity") so notification can share the Kafka event.
func (r *ActivityRepository) CreateWithProcessed(
	ctx context.Context,
	row *model.ActivityFeed,
	eventID, topic string,
) error {
	return r.db.WithContext(ctx).Transaction(func(tx *gorm.DB) error {
		var count int64
		if err := tx.Model(&model.ProcessedEvent{}).Where("event_id = ?", eventID).Count(&count).Error; err != nil {
			return err
		}
		if count > 0 {
			return nil
		}
		if row.CreatedAt.IsZero() {
			row.CreatedAt = time.Now().UTC()
		}
		if err := tx.Create(row).Error; err != nil {
			return err
		}
		return tx.Create(&model.ProcessedEvent{
			EventID:     eventID,
			Topic:       topic,
			ProcessedAt: time.Now().UTC(),
		}).Error
	})
}

func (r *ActivityRepository) ListByEntity(
	ctx context.Context,
	entityType string,
	entityID uuid.UUID,
	orgID *uuid.UUID,
	page, size int,
) ([]model.ActivityFeed, int64, error) {
	q := r.db.WithContext(ctx).Model(&model.ActivityFeed{}).
		Where("entity_type = ? AND entity_id = ?", entityType, entityID)
	if orgID != nil {
		q = q.Where("organization_id = ?", *orgID)
	}
	var total int64
	if err := q.Count(&total).Error; err != nil {
		return nil, 0, err
	}
	var list []model.ActivityFeed
	err := q.Order("created_at DESC").Offset(page * size).Limit(size).Find(&list).Error
	return list, total, err
}

func (r *ActivityRepository) ListByUser(
	ctx context.Context,
	userID uuid.UUID,
	orgID *uuid.UUID,
	page, size int,
) ([]model.ActivityFeed, int64, error) {
	q := r.db.WithContext(ctx).Model(&model.ActivityFeed{}).Where("user_id = ?", userID)
	if orgID != nil {
		q = q.Where("organization_id = ?", *orgID)
	}
	var total int64
	if err := q.Count(&total).Error; err != nil {
		return nil, 0, err
	}
	var list []model.ActivityFeed
	err := q.Order("created_at DESC").Offset(page * size).Limit(size).Find(&list).Error
	return list, total, err
}

func (r *ActivityRepository) ListByOrg(ctx context.Context, orgID uuid.UUID, page, size int) ([]model.ActivityFeed, int64, error) {
	q := r.db.WithContext(ctx).Model(&model.ActivityFeed{}).Where("organization_id = ?", orgID)
	var total int64
	if err := q.Count(&total).Error; err != nil {
		return nil, 0, err
	}
	var list []model.ActivityFeed
	err := q.Order("created_at DESC").Offset(page * size).Limit(size).Find(&list).Error
	return list, total, err
}
