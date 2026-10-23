package model

import (
	"time"

	"github.com/google/uuid"
)

type Comment struct {
	ID              uuid.UUID  `gorm:"type:uuid;primaryKey;default:gen_random_uuid()"`
	Content         string     `gorm:"type:text;not null"`
	EntityType      string     `gorm:"size:20;not null;index:idx_comments_entity,priority:1"`
	EntityID        uuid.UUID  `gorm:"type:uuid;not null;index:idx_comments_entity,priority:2"`
	AuthorID        uuid.UUID  `gorm:"type:uuid;not null;index:idx_comments_author_id"`
	ParentCommentID *uuid.UUID `gorm:"type:uuid;index:idx_comments_parent_comment_id"`
	IsInternal      bool       `gorm:"not null;default:false"`
	CreatedAt       time.Time  `gorm:"not null"`
	UpdatedAt       time.Time  `gorm:"not null"`
}

func (Comment) TableName() string { return "comments" }

type CommentMention struct {
	ID              uuid.UUID `gorm:"type:uuid;primaryKey;default:gen_random_uuid()"`
	CommentID       uuid.UUID `gorm:"type:uuid;not null;uniqueIndex:uq_comment_mention,priority:1"`
	MentionedUserID uuid.UUID `gorm:"type:uuid;not null;uniqueIndex:uq_comment_mention,priority:2;index:idx_comment_mentions_mentioned_user_id"`
	CreatedAt       time.Time `gorm:"not null"`
}

func (CommentMention) TableName() string { return "comment_mentions" }
