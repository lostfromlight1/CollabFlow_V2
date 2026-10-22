package repository

import (
	"context"
	"fmt"
	"time"

	"github.com/collabflow/collab-service/internal/model"
	"github.com/google/uuid"
	"gorm.io/gorm"
)

// Seeded platform role IDs (same as Java SystemRoles).
var (
	RoleOrgMember       = uuid.MustParse("a0000000-0000-4000-8000-000000000003")
	RoleWorkspaceMember = uuid.MustParse("a0000000-0000-4000-8000-000000000005")
	RoleProjectMember   = uuid.MustParse("a0000000-0000-4000-8000-000000000008")
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

func (r *InvitationRepository) ListByEmail(ctx context.Context, email string) ([]model.Invitation, error) {
	var list []model.Invitation
	err := r.db.WithContext(ctx).
		Where("email = ? AND status = ?", email, model.InvitePending).
		Order("created_at DESC").
		Find(&list).Error
	return list, err
}

func (r *InvitationRepository) ListByOrg(ctx context.Context, orgID uuid.UUID) ([]model.Invitation, error) {
	var list []model.Invitation
	err := r.db.WithContext(ctx).
		Where("organization_id = ?", orgID).
		Order("created_at DESC").
		Find(&list).Error
	return list, err
}

func (r *InvitationRepository) UpdateStatus(ctx context.Context, id uuid.UUID, status string) error {
	return r.db.WithContext(ctx).Model(&model.Invitation{}).
		Where("id = ?", id).Update("status", status).Error
}

func (r *InvitationRepository) FindUser(ctx context.Context, userID uuid.UUID) (*model.UserRef, error) {
	var u model.UserRef
	err := r.db.WithContext(ctx).First(&u, "id = ?", userID).Error
	if err != nil {
		return nil, err
	}
	return &u, nil
}

func (r *InvitationRepository) FindUserByEmail(ctx context.Context, email string) (*model.UserRef, error) {
	var u model.UserRef
	err := r.db.WithContext(ctx).Where("LOWER(email) = LOWER(?)", email).First(&u).Error
	if err != nil {
		return nil, err
	}
	return &u, nil
}

// AcceptInTx marks invitation accepted and adds membership rows (shared platform tables).
func (r *InvitationRepository) AcceptInTx(ctx context.Context, inv *model.Invitation, userID uuid.UUID) error {
	return r.db.WithContext(ctx).Transaction(func(tx *gorm.DB) error {
		if err := tx.Model(&model.Invitation{}).Where("id = ? AND status = ?", inv.ID, model.InvitePending).
			Update("status", model.InviteAccepted).Error; err != nil {
			return err
		}

		now := time.Now().UTC()
		// organization_members
		if err := ensureOrgMember(tx, inv.OrganizationID, userID, RoleOrgMember, now); err != nil {
			return err
		}
		if inv.InvitationType == model.InviteTypeWorkspace || inv.InvitationType == model.InviteTypeProject {
			if inv.WorkspaceID == nil {
				return fmt.Errorf("workspaceId required for %s invite", inv.InvitationType)
			}
			if err := ensureWorkspaceMember(tx, *inv.WorkspaceID, userID, RoleWorkspaceMember, now); err != nil {
				return err
			}
		}
		if inv.InvitationType == model.InviteTypeProject {
			if inv.ProjectID == nil {
				return fmt.Errorf("projectId required for PROJECT invite")
			}
			if err := ensureProjectMember(tx, *inv.ProjectID, userID, RoleProjectMember, now); err != nil {
				return err
			}
		}
		return nil
	})
}

func ensureOrgMember(tx *gorm.DB, orgID, userID, roleID uuid.UUID, now time.Time) error {
	var n int64
	tx.Table("organization_members").Where("organization_id = ? AND user_id = ?", orgID, userID).Count(&n)
	if n > 0 {
		return nil
	}
	return tx.Exec(`
		INSERT INTO organization_members (id, organization_id, user_id, role_id, joined_at, last_active_at)
		VALUES (gen_random_uuid(), ?, ?, ?, ?, ?)
	`, orgID, userID, roleID, now, now).Error
}

func ensureWorkspaceMember(tx *gorm.DB, wsID, userID, roleID uuid.UUID, now time.Time) error {
	var n int64
	tx.Table("workspace_members").Where("workspace_id = ? AND user_id = ?", wsID, userID).Count(&n)
	if n > 0 {
		return nil
	}
	return tx.Exec(`
		INSERT INTO workspace_members (id, workspace_id, user_id, role_id, joined_at, last_active_at)
		VALUES (gen_random_uuid(), ?, ?, ?, ?, ?)
	`, wsID, userID, roleID, now, now).Error
}

func ensureProjectMember(tx *gorm.DB, projectID, userID, roleID uuid.UUID, now time.Time) error {
	var n int64
	tx.Table("project_members").Where("project_id = ? AND user_id = ?", projectID, userID).Count(&n)
	if n > 0 {
		return nil
	}
	return tx.Exec(`
		INSERT INTO project_members (id, project_id, user_id, role_id, joined_at)
		VALUES (gen_random_uuid(), ?, ?, ?, ?)
	`, projectID, userID, roleID, now).Error
}
