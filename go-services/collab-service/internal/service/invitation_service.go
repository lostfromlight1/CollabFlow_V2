package service

import (
	"context"
	"crypto/rand"
	"encoding/hex"
	"errors"
	"fmt"
	"strings"
	"time"

	"github.com/collabflow/collab-service/internal/model"
	"github.com/collabflow/collab-service/internal/repository"
	"github.com/google/uuid"
	"gorm.io/gorm"
)

var (
	ErrInviteNotFound = errors.New("invitation not found")
	ErrInviteExpired  = errors.New("invitation expired")
	ErrInviteNotPending = errors.New("invitation is not pending")
	ErrInviteEmailMismatch = errors.New("logged-in user email does not match invitation")
	ErrInviteForbidden = errors.New("not allowed")
)

type CreateInvitationInput struct {
	Email          string
	OrganizationID uuid.UUID
	WorkspaceID    *uuid.UUID
	ProjectID      *uuid.UUID
	InvitationType string
}

type InvitationService struct {
	repo *repository.InvitationRepository
}

func NewInvitationService(repo *repository.InvitationRepository) *InvitationService {
	return &InvitationService{repo: repo}
}

func (s *InvitationService) Create(ctx context.Context, actorID uuid.UUID, in CreateInvitationInput) (*model.Invitation, error) {
	email := strings.TrimSpace(strings.ToLower(in.Email))
	if email == "" || in.OrganizationID == uuid.Nil {
		return nil, fmt.Errorf("email and organizationId required")
	}
	typ := strings.ToUpper(strings.TrimSpace(in.InvitationType))
	if typ == "" {
		typ = model.InviteTypeOrg
	}
	switch typ {
	case model.InviteTypeOrg:
		in.WorkspaceID, in.ProjectID = nil, nil
	case model.InviteTypeWorkspace:
		if in.WorkspaceID == nil {
			return nil, fmt.Errorf("workspaceId required for WORKSPACE invite")
		}
		in.ProjectID = nil
	case model.InviteTypeProject:
		if in.WorkspaceID == nil || in.ProjectID == nil {
			return nil, fmt.Errorf("workspaceId and projectId required for PROJECT invite")
		}
	default:
		return nil, fmt.Errorf("invalid invitationType")
	}

	token, err := randomToken(32)
	if err != nil {
		return nil, err
	}
	inv := &model.Invitation{
		Email:          email,
		OrganizationID: in.OrganizationID,
		WorkspaceID:    in.WorkspaceID,
		ProjectID:      in.ProjectID,
		InvitedBy:      actorID,
		InvitationType: typ,
		Status:         model.InvitePending,
		Token:          token,
		ExpiresAt:      time.Now().UTC().Add(7 * 24 * time.Hour),
		CreatedAt:      time.Now().UTC(),
	}
	if err := s.repo.Create(ctx, inv); err != nil {
		return nil, err
	}
	return inv, nil
}

func (s *InvitationService) GetByToken(ctx context.Context, token string) (*model.Invitation, error) {
	inv, err := s.repo.FindByToken(ctx, token)
	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return nil, ErrInviteNotFound
		}
		return nil, err
	}
	s.expireIfNeeded(ctx, inv)
	return inv, nil
}

func (s *InvitationService) ListMine(ctx context.Context, userID uuid.UUID, email string) ([]model.Invitation, error) {
	if email == "" {
		u, err := s.repo.FindUser(ctx, userID)
		if err != nil {
			return nil, err
		}
		email = u.Email
	}
	return s.repo.ListByEmail(ctx, strings.ToLower(email))
}

func (s *InvitationService) ListByOrg(ctx context.Context, orgID uuid.UUID) ([]model.Invitation, error) {
	return s.repo.ListByOrg(ctx, orgID)
}

func (s *InvitationService) Accept(ctx context.Context, userID uuid.UUID, email, token string) (*model.Invitation, error) {
	inv, err := s.repo.FindByToken(ctx, token)
	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return nil, ErrInviteNotFound
		}
		return nil, err
	}
	if err := s.ensureAcceptable(ctx, inv, email); err != nil {
		return nil, err
	}
	if err := s.repo.AcceptInTx(ctx, inv, userID); err != nil {
		return nil, err
	}
	inv.Status = model.InviteAccepted
	return inv, nil
}

func (s *InvitationService) Decline(ctx context.Context, userID uuid.UUID, email string, id uuid.UUID) error {
	inv, err := s.repo.FindByID(ctx, id)
	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return ErrInviteNotFound
		}
		return err
	}
	if err := s.ensureAcceptable(ctx, inv, email); err != nil {
		return err
	}
	return s.repo.UpdateStatus(ctx, inv.ID, model.InviteDeclined)
}

func (s *InvitationService) ensureAcceptable(ctx context.Context, inv *model.Invitation, email string) error {
	s.expireIfNeeded(ctx, inv)
	if inv.Status == model.InviteExpired {
		return ErrInviteExpired
	}
	if inv.Status != model.InvitePending {
		return ErrInviteNotPending
	}
	if time.Now().UTC().After(inv.ExpiresAt) {
		_ = s.repo.UpdateStatus(ctx, inv.ID, model.InviteExpired)
		return ErrInviteExpired
	}
	if email == "" {
		return ErrInviteEmailMismatch
	}
	if !strings.EqualFold(email, inv.Email) {
		return ErrInviteEmailMismatch
	}
	return nil
}

func (s *InvitationService) expireIfNeeded(ctx context.Context, inv *model.Invitation) {
	if inv.Status == model.InvitePending && time.Now().UTC().After(inv.ExpiresAt) {
		_ = s.repo.UpdateStatus(ctx, inv.ID, model.InviteExpired)
		inv.Status = model.InviteExpired
	}
}

func randomToken(nBytes int) (string, error) {
	b := make([]byte, nBytes)
	if _, err := rand.Read(b); err != nil {
		return "", err
	}
	return hex.EncodeToString(b), nil
}
