package repository

import (
	"context"
	"fmt"
	"time"

	"github.com/collabflow/collab-service/internal/model"
	"github.com/google/uuid"
	"gorm.io/gorm"
)

type CommentRepository struct {
	db *gorm.DB
}

func NewCommentRepository(db *gorm.DB) *CommentRepository {
	return &CommentRepository{db: db}
}

func (r *CommentRepository) CreateWithMentions(ctx context.Context, c *model.Comment, mentions []uuid.UUID) error {
	return r.db.WithContext(ctx).Transaction(func(tx *gorm.DB) error {
		now := time.Now().UTC()
		c.CreatedAt = now
		c.UpdatedAt = now
		if err := tx.Create(c).Error; err != nil {
			return err
		}
		for _, uid := range uniqueUUIDs(mentions) {
			m := model.CommentMention{
				CommentID:       c.ID,
				MentionedUserID: uid,
				CreatedAt:       now,
			}
			if err := tx.Create(&m).Error; err != nil {
				return err
			}
		}
		return nil
	})
}

func (r *CommentRepository) FindByID(ctx context.Context, id uuid.UUID) (*model.Comment, error) {
	var c model.Comment
	err := r.db.WithContext(ctx).First(&c, "id = ?", id).Error
	if err != nil {
		return nil, err
	}
	return &c, nil
}

func (r *CommentRepository) ListByEntity(ctx context.Context, entityType string, entityID uuid.UUID) ([]model.Comment, error) {
	var list []model.Comment
	err := r.db.WithContext(ctx).
		Where("entity_type = ? AND entity_id = ?", entityType, entityID).
		Order("created_at ASC").
		Find(&list).Error
	return list, err
}

func (r *CommentRepository) Update(ctx context.Context, c *model.Comment) error {
	c.UpdatedAt = time.Now().UTC()
	return r.db.WithContext(ctx).Save(c).Error
}

func (r *CommentRepository) Delete(ctx context.Context, id uuid.UUID) error {
	return r.db.WithContext(ctx).Transaction(func(tx *gorm.DB) error {
		if err := tx.Where("comment_id = ?", id).Delete(&model.CommentMention{}).Error; err != nil {
			return err
		}
		return tx.Delete(&model.Comment{}, "id = ?", id).Error
	})
}

func (r *CommentRepository) OrganizationIDForEntity(ctx context.Context, entityType string, entityID uuid.UUID) (uuid.UUID, error) {
	switch entityType {
	case "TASK":
		var t model.TaskRef
		if err := r.db.WithContext(ctx).First(&t, "id = ?", entityID).Error; err != nil {
			return uuid.Nil, err
		}
		return t.OrganizationID, nil
	case "PROJECT":
		var orgID uuid.UUID
		err := r.db.WithContext(ctx).Raw(`
			SELECT w.organization_id
			FROM projects p
			JOIN workspaces w ON w.id = p.workspace_id
			WHERE p.id = ?
		`, entityID).Scan(&orgID).Error
		if err != nil {
			return uuid.Nil, err
		}
		if orgID == uuid.Nil {
			return uuid.Nil, gorm.ErrRecordNotFound
		}
		return orgID, nil
	default:
		return uuid.Nil, fmt.Errorf("unsupported entityType %s", entityType)
	}
}

func uniqueUUIDs(ids []uuid.UUID) []uuid.UUID {
	seen := map[uuid.UUID]struct{}{}
	out := make([]uuid.UUID, 0, len(ids))
	for _, id := range ids {
		if _, ok := seen[id]; ok {
			continue
		}
		seen[id] = struct{}{}
		out = append(out, id)
	}
	return out
}
