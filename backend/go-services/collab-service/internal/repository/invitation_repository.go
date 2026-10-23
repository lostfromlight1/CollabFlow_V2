package repository

import (
	"context"

	"github.com/collabflow/collab-service/internal/model"
	"github.com/google/uuid"
	"gorm.io/gorm"
)

type InvitationRepository struct {
	db *gorm.DB
}

func NewInvitationRepository(db *gorm.DB) *InvitationRepository {
	return &InvitationRepository{db: db}
}

func (r *InvitationRepository) Create(ctx context.Context, inv *model.Invitation) error {
	return r.db.WithContext(ctx).Create(inv).Error
}

func (r *InvitationRepository) FindByToken(ctx context.Context, token string) (*model.Invitation, error) {
	var inv model.Invitation
	err := r.db.WithContext(ctx).Where("token = ?", token).First(&inv).Error
	if err != nil {
		return nil, err
	}
	return &inv, nil
}

func (r *InvitationRepository) FindByID(ctx context.Context, id uuid.UUID) (*model.Invitation, error) {
	var inv model.Invitation
	err := r.db.WithContext(ctx).First(&inv, "id = ?", id).Error
	if err != nil {
		return nil, err
	}
	return &inv, nil
}

func (r *InvitationRepository) ListByEmail(ctx context.Context, email string, orgID *uuid.UUID, page, size int) ([]model.Invitation, int64, error) {
	q := r.db.WithContext(ctx).Model(&model.Invitation{}).
		Where("email = ? AND status = ?", email, model.InvitePending)
	if orgID != nil {
		q = q.Where("organization_id = ?", *orgID)
	}
	var total int64
	if err := q.Count(&total).Error; err != nil {
		return nil, 0, err
	}
	var list []model.Invitation
	err := q.Order("created_at DESC").Offset(page * size).Limit(size).Find(&list).Error
	return list, total, err
}

func (r *InvitationRepository) ListByOrg(ctx context.Context, orgID uuid.UUID, page, size int) ([]model.Invitation, int64, error) {
	q := r.db.WithContext(ctx).Model(&model.Invitation{}).Where("organization_id = ?", orgID)
	var total int64
	if err := q.Count(&total).Error; err != nil {
		return nil, 0, err
	}
	var list []model.Invitation
	err := q.Order("created_at DESC").Offset(page * size).Limit(size).Find(&list).Error
	return list, total, err
}

func (r *InvitationRepository) UpdateStatus(ctx context.Context, id uuid.UUID, status string) error {
	return r.db.WithContext(ctx).Model(&model.Invitation{}).
		Where("id = ?", id).Update("status", status).Error
}

// MarkAcceptedIfPending sets status=ACCEPTED only when still PENDING (optimistic guard).
func (r *InvitationRepository) MarkAcceptedIfPending(ctx context.Context, id uuid.UUID) error {
	res := r.db.WithContext(ctx).Model(&model.Invitation{}).
		Where("id = ? AND status = ?", id, model.InvitePending).
		Update("status", model.InviteAccepted)
	if res.Error != nil {
		return res.Error
	}
	if res.RowsAffected == 0 {
		return gorm.ErrRecordNotFound
	}
	return nil
}

func (r *InvitationRepository) FindUser(ctx context.Context, userID uuid.UUID) (*model.UserRef, error) {
	var u model.UserRef
	err := r.db.WithContext(ctx).First(&u, "id = ?", userID).Error
	if err != nil {
		return nil, err
	}
	return &u, nil
}
