package service

import (
	"context"
	"errors"
	"fmt"
	"regexp"
	"strings"
	"time"

	"github.com/collabflow/collab-service/internal/kafka"
	"github.com/collabflow/collab-service/internal/model"
	"github.com/collabflow/collab-service/internal/repository"
	"github.com/google/uuid"
	"gorm.io/gorm"
)

var (
	ErrCommentNotFound = errors.New("comment not found")
	ErrCommentForbidden = errors.New("not the comment author")
	mentionPattern      = regexp.MustCompile(`@([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})`)
)

type CreateCommentInput struct {
	Content         string
	EntityType      string
	EntityID        uuid.UUID
	ParentCommentID *uuid.UUID
}

type CommentService struct {
	comments *repository.CommentRepository
	notifs   *repository.NotificationRepository
	producer *kafka.Producer
}

func NewCommentService(
	comments *repository.CommentRepository,
	notifs *repository.NotificationRepository,
	producer *kafka.Producer,
) *CommentService {
	return &CommentService{comments: comments, notifs: notifs, producer: producer}
}

func (s *CommentService) Create(ctx context.Context, authorID uuid.UUID, in CreateCommentInput) (*model.Comment, error) {
	entityType := strings.ToUpper(strings.TrimSpace(in.EntityType))
	if entityType != "TASK" && entityType != "PROJECT" {
		return nil, fmt.Errorf("entityType must be TASK or PROJECT")
	}
	content := strings.TrimSpace(in.Content)
	if content == "" || in.EntityID == uuid.Nil {
		return nil, fmt.Errorf("content and entityId required")
	}

	orgID, err := s.comments.OrganizationIDForEntity(ctx, entityType, in.EntityID)
	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return nil, fmt.Errorf("entity not found")
		}
		return nil, err
	}

	mentions := parseMentions(content)
	c := &model.Comment{
		Content:         content,
		EntityType:      entityType,
		EntityID:        in.EntityID,
		AuthorID:        authorID,
		ParentCommentID: in.ParentCommentID,
	}
	if err := s.comments.CreateWithMentions(ctx, c, mentions); err != nil {
		return nil, err
	}

	// Side effects after successful write (best-effort publish / notify)
	_ = s.producer.Publish(ctx, "comment.added", c.ID.String(), map[string]any{
		"commentId":  c.ID.String(),
		"entityType": c.EntityType,
		"entityId":   c.EntityID.String(),
		"authorId":   authorID.String(),
	})

	for _, mentioned := range mentions {
		if mentioned == authorID {
			continue
		}
		actionURL := fmt.Sprintf("/%s/%s", strings.ToLower(entityType)+"s", in.EntityID)
		title := "You were mentioned"
		msg := "Someone mentioned you in a comment"
		n := &model.Notification{
			RecipientID:      mentioned,
			SenderID:         &authorID,
			NotificationType: "COMMENT_MENTION",
			Title:            title,
			Message:          msg,
			EntityType:       entityTypeToNotifEntity(entityType),
			EntityID:         in.EntityID,
			OrganizationID:   orgID,
			IsRead:           false,
			ActionURL:        &actionURL,
			CreatedAt:        time.Now().UTC(),
		}
		_ = s.notifs.Create(ctx, n)
	}
	return c, nil
}

func (s *CommentService) Get(ctx context.Context, id uuid.UUID) (*model.Comment, error) {
	c, err := s.comments.FindByID(ctx, id)
	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return nil, ErrCommentNotFound
		}
		return nil, err
	}
	return c, nil
}

func (s *CommentService) List(ctx context.Context, entityType string, entityID uuid.UUID) ([]model.Comment, error) {
	return s.comments.ListByEntity(ctx, strings.ToUpper(entityType), entityID)
}

func (s *CommentService) Update(ctx context.Context, authorID, id uuid.UUID, content string) (*model.Comment, error) {
	c, err := s.Get(ctx, id)
	if err != nil {
		return nil, err
	}
	if c.AuthorID != authorID {
		return nil, ErrCommentForbidden
	}
	c.Content = strings.TrimSpace(content)
	if c.Content == "" {
		return nil, fmt.Errorf("content required")
	}
	if err := s.comments.Update(ctx, c); err != nil {
		return nil, err
	}
	return c, nil
}

func (s *CommentService) Delete(ctx context.Context, authorID, id uuid.UUID) error {
	c, err := s.Get(ctx, id)
	if err != nil {
		return err
	}
	if c.AuthorID != authorID {
		return ErrCommentForbidden
	}
	return s.comments.Delete(ctx, id)
}

func parseMentions(content string) []uuid.UUID {
	matches := mentionPattern.FindAllStringSubmatch(content, -1)
	out := make([]uuid.UUID, 0, len(matches))
	for _, m := range matches {
		if len(m) < 2 {
			continue
		}
		id, err := uuid.Parse(m[1])
		if err != nil {
			continue
		}
		out = append(out, id)
	}
	return out
}

func entityTypeToNotifEntity(entityType string) string {
	// notifications.entity_type CHECK allows TASK, PROJECT, ...
	return entityType
}
