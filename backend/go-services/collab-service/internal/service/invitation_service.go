package service

import (
	"context"
	"crypto/rand"
	"encoding/hex"
	"errors"
	"fmt"
	"log"
	"strings"
	"time"

	"github.com/collabflow/collab-service/internal/kafka"
	"github.com/collabflow/collab-service/internal/model"
	"github.com/collabflow/collab-service/internal/platform"
	"github.com/collabflow/collab-service/internal/repository"
	"github.com/google/uuid"
	"gorm.io/gorm"
)

var (
	ErrInviteNotFound      = errors.New("invitation not found")
	ErrInviteExpired       = errors.New("invitation expired")
	ErrInviteNotPending    = errors.New("invitation is not pending")
	ErrInviteEmailMismatch = errors.New("logged-in user email does not match invitation")
	ErrInviteForbidden     = errors.New("not allowed")
	ErrInvitePlatform      = errors.New("platform membership failed")
)

type CreateInvitationInput struct {
	Email          string
	OrganizationID uuid.UUID
	WorkspaceID    *uuid.UUID
	ProjectID      *uuid.UUID
	InvitationType string
}

type InvitationService struct {
	repo       *repository.InvitationRepository
	platform   *platform.Client
	producer   *kafka.Producer
	appPublic  string
}

func NewInvitationService(
	repo *repository.InvitationRepository,
	platformClient *platform.Client,
	producer *kafka.Producer,
	appPublicURL string,
) *InvitationService {
	return &InvitationService{
		repo:      repo,
		platform:  platformClient,
		producer:  producer,
		appPublic: strings.TrimRight(appPublicURL, "/"),
	}
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

	// After DB commit: publish email.send (auth uses the same DomainEvent envelope).
	// Collab's own email.send consumer stubs delivery until Wave 8 SMTP.
	s.publishInvitationEmail(ctx, inv)
	return inv, nil
}

func (s *InvitationService) publishInvitationEmail(ctx context.Context, inv *model.Invitation) {
	if s.producer == nil {
		return
	}
	link := fmt.Sprintf("%s/invite/%s", s.appPublic, inv.Token)
	subject := "You're invited to CollabFlow"
	body := fmt.Sprintf(
		"You have been invited to join an organization on CollabFlow.\n\nAccept: %s\n\nThis invitation expires at %s (UTC).",
		link,
		inv.ExpiresAt.Format(time.RFC3339),
	)
	data := kafka.EmailSendData{
		To:            inv.Email,
		Subject:       subject,
		Body:          body,
		Template:      "invitation",
		CorrelationID: inv.ID.String(),
	}
	if err := s.producer.Publish(ctx, "email.send", inv.ID.String(), data); err != nil {
		log.Printf("invitation email.send publish failed inviteId=%s: %v", inv.ID, err)
	}
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

func (s *InvitationService) ListMine(ctx context.Context, userID uuid.UUID, email string, orgID *uuid.UUID, page, size int) ([]model.Invitation, int64, error) {
	if email == "" {
		u, err := s.repo.FindUser(ctx, userID)
		if err != nil {
			return nil, 0, err
		}
		email = u.Email
	}
	return s.repo.ListByEmail(ctx, strings.ToLower(email), orgID, page, size)
}

func (s *InvitationService) ListByOrg(ctx context.Context, orgID uuid.UUID, page, size int) ([]model.Invitation, int64, error) {
	return s.repo.ListByOrg(ctx, orgID, page, size)
}

// Accept validates the invite, calls platform membership APIs (not shared-DB writes), then marks ACCEPTED.
// bearerToken: Authorization from the accept request (forwarded to platform with X-API-Key).
func (s *InvitationService) Accept(ctx context.Context, userID uuid.UUID, email, token, bearerToken string) (*model.Invitation, error) {
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

	if err := s.applyPlatformMembership(ctx, inv, userID, bearerToken); err != nil {
		return nil, fmt.Errorf("%w: %v", ErrInvitePlatform, err)
	}

	if err := s.repo.MarkAcceptedIfPending(ctx, inv.ID); err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return nil, ErrInviteNotPending
		}
		return nil, err
	}
	inv.Status = model.InviteAccepted
	return inv, nil
}

func (s *InvitationService) applyPlatformMembership(ctx context.Context, inv *model.Invitation, userID uuid.UUID, bearerToken string) error {
	if s.platform == nil {
		return fmt.Errorf("platform client not configured")
	}
	// Always ensure org membership first (platform owns organization_members).
	if err := s.platform.AddOrgMember(ctx, inv.OrganizationID, userID, platform.RoleOrgMember, bearerToken); err != nil {
		return err
	}
	if inv.InvitationType == model.InviteTypeWorkspace || inv.InvitationType == model.InviteTypeProject {
		if inv.WorkspaceID == nil {
			return fmt.Errorf("workspaceId required for %s invite", inv.InvitationType)
		}
		if err := s.platform.AddWorkspaceMember(ctx, *inv.WorkspaceID, userID, platform.RoleWorkspaceMember, bearerToken); err != nil {
			return err
		}
	}
	if inv.InvitationType == model.InviteTypeProject {
		if inv.ProjectID == nil {
			return fmt.Errorf("projectId required for PROJECT invite")
		}
		if err := s.platform.AddProjectMember(ctx, *inv.ProjectID, userID, platform.RoleProjectMember, bearerToken); err != nil {
			return err
		}
	}
	return nil
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
