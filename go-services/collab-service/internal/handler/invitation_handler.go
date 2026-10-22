package handler

import (
	"errors"
	"net/http"
	"strings"
	"time"

	"github.com/collabflow/collab-service/internal/middleware"
	"github.com/collabflow/collab-service/internal/model"
	"github.com/collabflow/collab-service/internal/service"
	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
)

type InvitationHandler struct {
	service *service.InvitationService
}

func NewInvitationHandler(service *service.InvitationService) *InvitationHandler {
	return &InvitationHandler{service: service}
}

type createInviteRequest struct {
	Email          string     `json:"email" binding:"required,email"`
	OrganizationID uuid.UUID  `json:"organizationId" binding:"required"`
	WorkspaceID    *uuid.UUID `json:"workspaceId"`
	ProjectID      *uuid.UUID `json:"projectId"`
	InvitationType string     `json:"invitationType"`
}

type invitationDTO struct {
	ID             uuid.UUID  `json:"id"`
	Email          string     `json:"email"`
	OrganizationID uuid.UUID  `json:"organizationId"`
	WorkspaceID    *uuid.UUID `json:"workspaceId"`
	ProjectID      *uuid.UUID `json:"projectId"`
	InvitedBy      uuid.UUID  `json:"invitedBy"`
	InvitationType string     `json:"invitationType"`
	Status         string     `json:"status"`
	Token          string     `json:"token"`
	ExpiresAt      time.Time  `json:"expiresAt"`
	CreatedAt      time.Time  `json:"createdAt"`
}

func inviteDTO(inv *model.Invitation) invitationDTO {
	return invitationDTO{
		ID:             inv.ID,
		Email:          inv.Email,
		OrganizationID: inv.OrganizationID,
		WorkspaceID:    inv.WorkspaceID,
		ProjectID:      inv.ProjectID,
		InvitedBy:      inv.InvitedBy,
		InvitationType: inv.InvitationType,
		Status:         inv.Status,
		Token:          inv.Token,
		ExpiresAt:      inv.ExpiresAt,
		CreatedAt:      inv.CreatedAt,
	}
}

func mapInviteList(list []model.Invitation) []invitationDTO {
	out := make([]invitationDTO, 0, len(list))
	for i := range list {
		out = append(out, inviteDTO(&list[i]))
	}
	return out
}

func (h *InvitationHandler) Create(c *gin.Context) {
	userID, ok := middleware.UserID(c)
	if !ok {
		unauthorized(c)
		return
	}
	var req createInviteRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"success": false, "message": err.Error(), "errorCode": "VALIDATION_FAILED"})
		return
	}
	// Prefer org header when present (Wave 2+ session org context).
	if org := optionalOrgHeader(c); org != nil {
		req.OrganizationID = *org
	}
	inv, err := h.service.Create(c.Request.Context(), userID, service.CreateInvitationInput{
		Email:          req.Email,
		OrganizationID: req.OrganizationID,
		WorkspaceID:    req.WorkspaceID,
		ProjectID:      req.ProjectID,
		InvitationType: req.InvitationType,
	})
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"success": false, "message": err.Error(), "errorCode": "BAD_REQUEST"})
		return
	}
	c.JSON(http.StatusCreated, gin.H{"success": true, "message": "Invitation created", "data": inviteDTO(inv)})
}

func (h *InvitationHandler) GetByToken(c *gin.Context) {
	inv, err := h.service.GetByToken(c.Request.Context(), c.Param("token"))
	if err != nil {
		mapInviteErr(c, err)
		return
	}
	c.JSON(http.StatusOK, gin.H{"success": true, "message": "OK", "data": inviteDTO(inv)})
}

func (h *InvitationHandler) ListMine(c *gin.Context) {
	userID, ok := middleware.UserID(c)
	if !ok {
		unauthorized(c)
		return
	}
	p := parsePage(c)
	list, total, err := h.service.ListMine(c.Request.Context(), userID, middleware.Email(c), optionalOrgHeader(c), p.Page, p.Size)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"success": false, "message": err.Error(), "errorCode": "INTERNAL_ERROR"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"success": true, "message": "OK", "data": pageEnvelope(mapInviteList(list), p.Page, p.Size, total)})
}

func (h *InvitationHandler) ListByOrg(c *gin.Context) {
	orgID, ok := organizationIDFromRequest(c)
	if !ok {
		c.JSON(http.StatusBadRequest, gin.H{
			"success":   false,
			"message":   "X-Organization-Id or organizationId query required",
			"errorCode": "BAD_REQUEST",
		})
		return
	}
	p := parsePage(c)
	list, total, err := h.service.ListByOrg(c.Request.Context(), orgID, p.Page, p.Size)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"success": false, "message": err.Error(), "errorCode": "INTERNAL_ERROR"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"success": true, "message": "OK", "data": pageEnvelope(mapInviteList(list), p.Page, p.Size, total)})
}

func (h *InvitationHandler) Accept(c *gin.Context) {
	userID, ok := middleware.UserID(c)
	if !ok {
		unauthorized(c)
		return
	}
	// Forward the same Bearer JWT to platform (plus X-API-Key from collab config).
	auth := c.GetHeader("Authorization")
	inv, err := h.service.Accept(c.Request.Context(), userID, middleware.Email(c), c.Param("token"), auth)
	if err != nil {
		mapInviteErr(c, err)
		return
	}
	c.JSON(http.StatusOK, gin.H{"success": true, "message": "Invitation accepted", "data": inviteDTO(inv)})
}

func (h *InvitationHandler) Decline(c *gin.Context) {
	userID, ok := middleware.UserID(c)
	if !ok {
		unauthorized(c)
		return
	}
	id, err := uuid.Parse(c.Param("id"))
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"success": false, "message": "Invalid id", "errorCode": "BAD_REQUEST"})
		return
	}
	if err := h.service.Decline(c.Request.Context(), userID, middleware.Email(c), id); err != nil {
		mapInviteErr(c, err)
		return
	}
	c.JSON(http.StatusOK, gin.H{"success": true, "message": "Invitation declined", "data": nil})
}

func unauthorized(c *gin.Context) {
	c.JSON(http.StatusUnauthorized, gin.H{"success": false, "message": "Unauthorized", "errorCode": "UNAUTHORIZED"})
}

func mapInviteErr(c *gin.Context, err error) {
	switch {
	case errors.Is(err, service.ErrInviteNotFound):
		c.JSON(http.StatusNotFound, gin.H{"success": false, "message": err.Error(), "errorCode": "NOT_FOUND"})
	case errors.Is(err, service.ErrInviteExpired), errors.Is(err, service.ErrInviteNotPending), errors.Is(err, service.ErrInviteEmailMismatch):
		c.JSON(http.StatusForbidden, gin.H{"success": false, "message": err.Error(), "errorCode": "FORBIDDEN"})
	case errors.Is(err, service.ErrInvitePlatform):
		// Surface platform body briefly for debugging (403 manage_members, etc.).
		msg := err.Error()
		if i := strings.Index(msg, ": "); i >= 0 {
			msg = strings.TrimSpace(msg[i+2:])
		}
		c.JSON(http.StatusBadGateway, gin.H{"success": false, "message": msg, "errorCode": "PLATFORM_ERROR"})
	default:
		c.JSON(http.StatusBadRequest, gin.H{"success": false, "message": err.Error(), "errorCode": "BAD_REQUEST"})
	}
}
