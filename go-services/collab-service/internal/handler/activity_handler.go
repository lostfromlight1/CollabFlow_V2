package handler

import (
	"encoding/json"
	"net/http"
	"strconv"
	"time"

	"github.com/collabflow/collab-service/internal/middleware"
	"github.com/collabflow/collab-service/internal/model"
	"github.com/collabflow/collab-service/internal/service"
	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
)

type ActivityHandler struct {
	service *service.ActivityService
}

func NewActivityHandler(service *service.ActivityService) *ActivityHandler {
	return &ActivityHandler{service: service}
}

type activityDTO struct {
	ID             uuid.UUID       `json:"id"`
	ActionType     string          `json:"actionType"`
	EntityType     string          `json:"entityType"`
	EntityID       uuid.UUID       `json:"entityId"`
	OrganizationID uuid.UUID       `json:"organizationId"`
	WorkspaceID    *uuid.UUID      `json:"workspaceId"`
	ProjectID      *uuid.UUID      `json:"projectId"`
	UserID         uuid.UUID       `json:"userId"`
	ActorName      string          `json:"actorName"`
	ActorEmail     string          `json:"actorEmail"`
	Changes        json.RawMessage `json:"changes"`
	Metadata       json.RawMessage `json:"metadata"`
	CreatedAt      time.Time       `json:"createdAt"`
}

func toActivityDTO(a *model.ActivityFeed) activityDTO {
	dto := activityDTO{
		ID:             a.ID,
		ActionType:     a.ActionType,
		EntityType:     a.EntityType,
		EntityID:       a.EntityID,
		OrganizationID: a.OrganizationID,
		WorkspaceID:    a.WorkspaceID,
		ProjectID:      a.ProjectID,
		UserID:         a.UserID,
		ActorName:      a.ActorName,
		ActorEmail:     a.ActorEmail,
		CreatedAt:      a.CreatedAt,
	}
	if len(a.Changes) > 0 {
		dto.Changes = json.RawMessage(a.Changes)
	}
	if len(a.Metadata) > 0 {
		dto.Metadata = json.RawMessage(a.Metadata)
	}
	return dto
}

func mapActivities(list []model.ActivityFeed) []activityDTO {
	out := make([]activityDTO, 0, len(list))
	for i := range list {
		out = append(out, toActivityDTO(&list[i]))
	}
	return out
}

func parseLimit(c *gin.Context) int {
	n, _ := strconv.Atoi(c.DefaultQuery("limit", "50"))
	return n
}

// List: GET /activity?organizationId= | entityType=&entityId= | userId=
func (h *ActivityHandler) List(c *gin.Context) {
	if _, ok := middleware.UserID(c); !ok {
		unauthorized(c)
		return
	}
	limit := parseLimit(c)
	ctx := c.Request.Context()

	if et, eid := c.Query("entityType"), c.Query("entityId"); et != "" && eid != "" {
		id, err := uuid.Parse(eid)
		if err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"success": false, "message": "invalid entityId", "errorCode": "BAD_REQUEST"})
			return
		}
		list, err := h.service.ListByEntity(ctx, et, id, limit)
		if err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"success": false, "message": err.Error(), "errorCode": "INTERNAL_ERROR"})
			return
		}
		c.JSON(http.StatusOK, gin.H{"success": true, "message": "OK", "data": mapActivities(list)})
		return
	}

	if uid := c.Query("userId"); uid != "" {
		id, err := uuid.Parse(uid)
		if err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"success": false, "message": "invalid userId", "errorCode": "BAD_REQUEST"})
			return
		}
		list, err := h.service.ListByUser(ctx, id, limit)
		if err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"success": false, "message": err.Error(), "errorCode": "INTERNAL_ERROR"})
			return
		}
		c.JSON(http.StatusOK, gin.H{"success": true, "message": "OK", "data": mapActivities(list)})
		return
	}

	if oid := c.Query("organizationId"); oid != "" {
		id, err := uuid.Parse(oid)
		if err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"success": false, "message": "invalid organizationId", "errorCode": "BAD_REQUEST"})
			return
		}
		list, err := h.service.ListByOrg(ctx, id, limit)
		if err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"success": false, "message": err.Error(), "errorCode": "INTERNAL_ERROR"})
			return
		}
		c.JSON(http.StatusOK, gin.H{"success": true, "message": "OK", "data": mapActivities(list)})
		return
	}

	c.JSON(http.StatusBadRequest, gin.H{
		"success":   false,
		"message":   "provide organizationId, or entityType+entityId, or userId",
		"errorCode": "BAD_REQUEST",
	})
}

// ListByEntity: GET /activity/entity/:type/:id
func (h *ActivityHandler) ListByEntity(c *gin.Context) {
	if _, ok := middleware.UserID(c); !ok {
		unauthorized(c)
		return
	}
	id, err := uuid.Parse(c.Param("id"))
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"success": false, "message": "invalid id", "errorCode": "BAD_REQUEST"})
		return
	}
	list, err := h.service.ListByEntity(c.Request.Context(), c.Param("type"), id, parseLimit(c))
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"success": false, "message": err.Error(), "errorCode": "INTERNAL_ERROR"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"success": true, "message": "OK", "data": mapActivities(list)})
}

// ListByUser: GET /activity/user/:id
func (h *ActivityHandler) ListByUser(c *gin.Context) {
	if _, ok := middleware.UserID(c); !ok {
		unauthorized(c)
		return
	}
	id, err := uuid.Parse(c.Param("id"))
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"success": false, "message": "invalid id", "errorCode": "BAD_REQUEST"})
		return
	}
	list, err := h.service.ListByUser(c.Request.Context(), id, parseLimit(c))
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"success": false, "message": err.Error(), "errorCode": "INTERNAL_ERROR"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"success": true, "message": "OK", "data": mapActivities(list)})
}
