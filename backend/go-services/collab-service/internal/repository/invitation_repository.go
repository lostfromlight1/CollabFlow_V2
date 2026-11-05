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

func (r *InvitationRepository) ListByEmail(ctx context.Context, email string, page, size int) ([]model.Invitation, int64, error) {
	q := r.db.WithContext(ctx).Model(&model.Invitation{}).
		Where("email = ? AND status = ?", email, model.InvitePending)
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

// CallerCanManage is a read of platform's permission rows.
// Java analogy: PermissionService.requireOrgPermission, without writing member tables.
// The workspace and project queries also require the row to belong to organizationID.
func (r *InvitationRepository) CallerCanManage(
	ctx context.Context,
	userID, organizationID uuid.UUID,
	workspaceID, projectID *uuid.UUID,
	invitationType string,
) (bool, error) {
	var n int64
	var err error
	switch invitationType {
	case model.InviteTypeOrg:
		err = r.db.WithContext(ctx).Raw(`
			SELECT COUNT(*) FROM organization_members om
			JOIN role_permissions rp ON rp.role_id = om.role_id
			JOIN permissions p ON p.id = rp.permission_id
			WHERE om.organization_id = ? AND om.user_id = ? AND p.name = ?
		`, organizationID, userID, "organization.manage_members").Scan(&n).Error
	case model.InviteTypeWorkspace:
		if workspaceID == nil {
			return false, nil
		}
		err = r.db.WithContext(ctx).Raw(`
			SELECT COUNT(*) FROM workspace_members wm
			JOIN workspaces w ON w.id = wm.workspace_id
			JOIN role_permissions rp ON rp.role_id = wm.role_id
			JOIN permissions p ON p.id = rp.permission_id
			WHERE wm.workspace_id = ? AND wm.user_id = ? AND w.organization_id = ? AND p.name = ?
		`, *workspaceID, userID, organizationID, "workspace.manage_members").Scan(&n).Error
	case model.InviteTypeProject:
		if projectID == nil {
			return false, nil
		}
		err = r.db.WithContext(ctx).Raw(`
			SELECT COUNT(*) FROM project_members pm
			JOIN projects pr ON pr.id = pm.project_id
			JOIN workspaces w ON w.id = pr.workspace_id
			JOIN role_permissions rp ON rp.role_id = pm.role_id
			JOIN permissions p ON p.id = rp.permission_id
			WHERE pm.project_id = ? AND pm.user_id = ? AND w.organization_id = ? AND p.name = ?
		`, *projectID, userID, organizationID, "project.manage_members").Scan(&n).Error
	default:
		return false, nil
	}
	if err != nil {
		return false, err
	}
	return n > 0, nil
}

func (r *InvitationRepository) FindUser(ctx context.Context, userID uuid.UUID) (*model.UserRef, error) {
	var u model.UserRef
	err := r.db.WithContext(ctx).First(&u, "id = ?", userID).Error
	if err != nil {
		return nil, err
	}
	return &u, nil
}
