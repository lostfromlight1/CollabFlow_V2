package acl

import (
	"context"
	"fmt"
	"strings"

	"github.com/google/uuid"
	"gorm.io/gorm"
)

// Checker answers "may this JWT user join this WebSocket room?"
// Java analogy: a thin PermissionService that runs EXISTS queries (no JPA entities).
type Checker struct {
	db *gorm.DB
}

func NewChecker(db *gorm.DB) *Checker {
	return &Checker{db: db}
}

// AllowSubscribe decides if userID may join room.
// optionalOrgID: when set (from X-Organization-Id), project/task must belong to that org.
func (c *Checker) AllowSubscribe(ctx context.Context, userID uuid.UUID, room string, optionalOrgID *uuid.UUID) error {
	prefix, id, err := parseRoom(room)
	if err != nil {
		return err
	}

	switch prefix {
	case "org":
		ok, err := c.isOrgMember(ctx, id, userID)
		if err != nil {
			return err
		}
		if !ok {
			return fmt.Errorf("not an organization member")
		}
		if optionalOrgID != nil && *optionalOrgID != id {
			return fmt.Errorf("org room does not match X-Organization-Id")
		}
		return nil

	case "project":
		if optionalOrgID != nil {
			belongs, err := c.projectBelongsToOrg(ctx, id, *optionalOrgID)
			if err != nil {
				return err
			}
			if !belongs {
				return fmt.Errorf("project not in selected organization")
			}
		}
		ok, err := c.canAccessProject(ctx, id, userID)
		if err != nil {
			return err
		}
		if !ok {
			return fmt.Errorf("not a project or organization member")
		}
		return nil

	case "task":
		projectID, orgID, err := c.taskScope(ctx, id)
		if err != nil {
			return err
		}
		if optionalOrgID != nil && orgID != *optionalOrgID {
			return fmt.Errorf("task not in selected organization")
		}
		ok, err := c.canAccessProject(ctx, projectID, userID)
		if err != nil {
			return err
		}
		if !ok {
			return fmt.Errorf("not a project or organization member")
		}
		return nil

	case "user":
		// Private room: only the JWT subject may subscribe to user:{self}.
		if id != userID {
			return fmt.Errorf("cannot subscribe to another user's room")
		}
		return nil

	default:
		return fmt.Errorf("unsupported room type %q", prefix)
	}
}

// IsOrgMember is used when binding X-Organization-Id on the WebSocket handshake.
func (c *Checker) IsOrgMember(ctx context.Context, orgID, userID uuid.UUID) (bool, error) {
	return c.isOrgMember(ctx, orgID, userID)
}

func (c *Checker) canAccessProject(ctx context.Context, projectID, userID uuid.UUID) (bool, error) {
	var n int64
	err := c.db.WithContext(ctx).Raw(`
		SELECT COUNT(*) FROM project_members
		WHERE project_id = ? AND user_id = ?
	`, projectID, userID).Scan(&n).Error
	if err != nil {
		return false, err
	}
	if n > 0 {
		return true, nil
	}
	// Fallback: any org member of the project's organization (MVP pragmatism).
	err = c.db.WithContext(ctx).Raw(`
		SELECT COUNT(*)
		FROM projects p
		JOIN workspaces w ON w.id = p.workspace_id
		JOIN organization_members om ON om.organization_id = w.organization_id AND om.user_id = ?
		WHERE p.id = ?
	`, userID, projectID).Scan(&n).Error
	if err != nil {
		return false, err
	}
	return n > 0, nil
}

func (c *Checker) isOrgMember(ctx context.Context, orgID, userID uuid.UUID) (bool, error) {
	var n int64
	err := c.db.WithContext(ctx).Raw(`
		SELECT COUNT(*) FROM organization_members
		WHERE organization_id = ? AND user_id = ?
	`, orgID, userID).Scan(&n).Error
	if err != nil {
		return false, err
	}
	return n > 0, nil
}

func (c *Checker) projectBelongsToOrg(ctx context.Context, projectID, orgID uuid.UUID) (bool, error) {
	var n int64
	err := c.db.WithContext(ctx).Raw(`
		SELECT COUNT(*)
		FROM projects p
		JOIN workspaces w ON w.id = p.workspace_id
		WHERE p.id = ? AND w.organization_id = ?
	`, projectID, orgID).Scan(&n).Error
	if err != nil {
		return false, err
	}
	return n > 0, nil
}

func (c *Checker) taskScope(ctx context.Context, taskID uuid.UUID) (projectID, orgID uuid.UUID, err error) {
	row := struct {
		ProjectID      uuid.UUID `gorm:"column:project_id"`
		OrganizationID uuid.UUID `gorm:"column:organization_id"`
	}{}
	err = c.db.WithContext(ctx).Raw(`
		SELECT project_id, organization_id FROM tasks WHERE id = ?
	`, taskID).Scan(&row).Error
	if err != nil {
		return uuid.Nil, uuid.Nil, err
	}
	if row.ProjectID == uuid.Nil {
		return uuid.Nil, uuid.Nil, fmt.Errorf("task not found")
	}
	return row.ProjectID, row.OrganizationID, nil
}

func parseRoom(room string) (prefix string, id uuid.UUID, err error) {
	room = strings.TrimSpace(room)
	parts := strings.SplitN(room, ":", 2)
	if len(parts) != 2 || parts[0] == "" || parts[1] == "" {
		return "", uuid.Nil, fmt.Errorf("invalid room format (expected type:uuid)")
	}
	id, err = uuid.Parse(parts[1])
	if err != nil {
		return "", uuid.Nil, fmt.Errorf("invalid room id")
	}
	return strings.ToLower(parts[0]), id, nil
}
