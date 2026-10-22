package handler

import (
	"net/http"
	"time"

	"github.com/collabflow/collab-service/internal/middleware"
	"github.com/collabflow/collab-service/internal/model"
	"github.com/collabflow/collab-service/internal/service"
	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
	"gorm.io/gorm"
)

type NotificationHandler struct {
	service *service.NotificationService
}

func NewNotificationHandler(service *service.NotificationService) *NotificationHandler {
	return &NotificationHandler{service: service}
}

type notificationDTO struct {
	ID               uuid.UUID  `json:"id"`
	RecipientID      uuid.UUID  `json:"recipientId"`
	SenderID         *uuid.UUID `json:"senderId"`
	NotificationType string     `json:"notificationType"`
	Title            string     `json:"title"`
	Message          string     `json:"message"`
	EntityType       string     `json:"entityType"`
	EntityID         uuid.UUID  `json:"entityId"`
	OrganizationID   uuid.UUID  `json:"organizationId"`
	IsRead           bool       `json:"isRead"`
	ReadAt           *time.Time `json:"readAt"`
	ActionURL        *string    `json:"actionUrl"`
	CreatedAt        time.Time  `json:"createdAt"`
}

func toDTO(n model.Notification) notificationDTO {
	return notificationDTO{
		ID:               n.ID,
		RecipientID:      n.RecipientID,
		SenderID:         n.SenderID,
		NotificationType: n.NotificationType,
		Title:            n.Title,
		Message:          n.Message,
		EntityType:       n.EntityType,
		EntityID:         n.EntityID,
		OrganizationID:   n.OrganizationID,
		IsRead:           n.IsRead,
		ReadAt:           n.ReadAt,
		ActionURL:        n.ActionURL,
		CreatedAt:        n.CreatedAt,
	}
}

func (h *NotificationHandler) List(c *gin.Context) {
	userID, ok := middleware.UserID(c)
	if !ok {
		c.JSON(http.StatusUnauthorized, gin.H{"success": false, "message": "Unauthorized", "errorCode": "UNAUTHORIZED"})
		return
	}
	list, err := h.service.ListMine(c.Request.Context(), userID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"success": false, "message": err.Error(), "errorCode": "INTERNAL_ERROR"})
		return
	}
	out := make([]notificationDTO, 0, len(list))
	for _, n := range list {
		out = append(out, toDTO(n))
	}
	c.JSON(http.StatusOK, gin.H{"success": true, "message": "OK", "data": out})
}

func (h *NotificationHandler) MarkRead(c *gin.Context) {
	userID, ok := middleware.UserID(c)
	if !ok {
		c.JSON(http.StatusUnauthorized, gin.H{"success": false, "message": "Unauthorized", "errorCode": "UNAUTHORIZED"})
		return
	}
	id, err := uuid.Parse(c.Param("id"))
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"success": false, "message": "Invalid id", "errorCode": "BAD_REQUEST"})
		return
	}
	if err := h.service.MarkRead(c.Request.Context(), userID, id); err != nil {
		if err == gorm.ErrRecordNotFound {
			c.JSON(http.StatusNotFound, gin.H{"success": false, "message": "Notification not found", "errorCode": "NOT_FOUND"})
			return
		}
		c.JSON(http.StatusInternalServerError, gin.H{"success": false, "message": err.Error(), "errorCode": "INTERNAL_ERROR"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"success": true, "message": "Marked read", "data": nil})
}

func (h *NotificationHandler) MarkAllRead(c *gin.Context) {
	userID, ok := middleware.UserID(c)
	if !ok {
		c.JSON(http.StatusUnauthorized, gin.H{"success": false, "message": "Unauthorized", "errorCode": "UNAUTHORIZED"})
		return
	}
	n, err := h.service.MarkAllRead(c.Request.Context(), userID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"success": false, "message": err.Error(), "errorCode": "INTERNAL_ERROR"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"success": true, "message": "Marked all read", "data": gin.H{"updated": n}})
}
