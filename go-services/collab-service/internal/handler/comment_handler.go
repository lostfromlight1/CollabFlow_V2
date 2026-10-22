package handler

import (
	"errors"
	"net/http"
	"time"

	"github.com/collabflow/collab-service/internal/middleware"
	"github.com/collabflow/collab-service/internal/model"
	"github.com/collabflow/collab-service/internal/service"
	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
)

type CommentHandler struct {
	service *service.CommentService
}

func NewCommentHandler(service *service.CommentService) *CommentHandler {
	return &CommentHandler{service: service}
}

type createCommentRequest struct {
	Content         string     `json:"content" binding:"required"`
	EntityType      string     `json:"entityType" binding:"required"`
	EntityID        uuid.UUID  `json:"entityId" binding:"required"`
	ParentCommentID *uuid.UUID `json:"parentCommentId"`
}

type updateCommentRequest struct {
	Content string `json:"content" binding:"required"`
}

type commentDTO struct {
	ID              uuid.UUID  `json:"id"`
	Content         string     `json:"content"`
	EntityType      string     `json:"entityType"`
	EntityID        uuid.UUID  `json:"entityId"`
	AuthorID        uuid.UUID  `json:"authorId"`
	ParentCommentID *uuid.UUID `json:"parentCommentId"`
	IsInternal      bool       `json:"isInternal"`
	CreatedAt       time.Time  `json:"createdAt"`
	UpdatedAt       time.Time  `json:"updatedAt"`
}

func commentToDTO(c *model.Comment) commentDTO {
	return commentDTO{
		ID:              c.ID,
		Content:         c.Content,
		EntityType:      c.EntityType,
		EntityID:        c.EntityID,
		AuthorID:        c.AuthorID,
		ParentCommentID: c.ParentCommentID,
		IsInternal:      c.IsInternal,
		CreatedAt:       c.CreatedAt,
		UpdatedAt:       c.UpdatedAt,
	}
}

func (h *CommentHandler) Create(c *gin.Context) {
	userID, ok := middleware.UserID(c)
	if !ok {
		unauthorized(c)
		return
	}
	var req createCommentRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"success": false, "message": err.Error(), "errorCode": "VALIDATION_FAILED"})
		return
	}
	created, err := h.service.Create(c.Request.Context(), userID, service.CreateCommentInput{
		Content:         req.Content,
		EntityType:      req.EntityType,
		EntityID:        req.EntityID,
		ParentCommentID: req.ParentCommentID,
	})
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"success": false, "message": err.Error(), "errorCode": "BAD_REQUEST"})
		return
	}
	c.JSON(http.StatusCreated, gin.H{"success": true, "message": "Comment created", "data": commentToDTO(created)})
}

func (h *CommentHandler) Get(c *gin.Context) {
	id, err := uuid.Parse(c.Param("id"))
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"success": false, "message": "Invalid id", "errorCode": "BAD_REQUEST"})
		return
	}
	cm, err := h.service.Get(c.Request.Context(), id)
	if err != nil {
		mapCommentErr(c, err)
		return
	}
	c.JSON(http.StatusOK, gin.H{"success": true, "message": "OK", "data": commentToDTO(cm)})
}

func (h *CommentHandler) List(c *gin.Context) {
	entityType := c.Query("entityType")
	entityID, err := uuid.Parse(c.Query("entityId"))
	if entityType == "" || err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"success": false, "message": "entityType and entityId required", "errorCode": "BAD_REQUEST"})
		return
	}
	list, err := h.service.List(c.Request.Context(), entityType, entityID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"success": false, "message": err.Error(), "errorCode": "INTERNAL_ERROR"})
		return
	}
	out := make([]commentDTO, 0, len(list))
	for i := range list {
		out = append(out, commentToDTO(&list[i]))
	}
	c.JSON(http.StatusOK, gin.H{"success": true, "message": "OK", "data": out})
}

func (h *CommentHandler) Update(c *gin.Context) {
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
	var req updateCommentRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"success": false, "message": err.Error(), "errorCode": "VALIDATION_FAILED"})
		return
	}
	cm, err := h.service.Update(c.Request.Context(), userID, id, req.Content)
	if err != nil {
		mapCommentErr(c, err)
		return
	}
	c.JSON(http.StatusOK, gin.H{"success": true, "message": "Updated", "data": commentToDTO(cm)})
}

func (h *CommentHandler) Delete(c *gin.Context) {
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
	if err := h.service.Delete(c.Request.Context(), userID, id); err != nil {
		mapCommentErr(c, err)
		return
	}
	c.JSON(http.StatusOK, gin.H{"success": true, "message": "Deleted", "data": nil})
}

func mapCommentErr(c *gin.Context, err error) {
	switch {
	case errors.Is(err, service.ErrCommentNotFound):
		c.JSON(http.StatusNotFound, gin.H{"success": false, "message": err.Error(), "errorCode": "NOT_FOUND"})
	case errors.Is(err, service.ErrCommentForbidden):
		c.JSON(http.StatusForbidden, gin.H{"success": false, "message": err.Error(), "errorCode": "FORBIDDEN"})
	default:
		c.JSON(http.StatusBadRequest, gin.H{"success": false, "message": err.Error(), "errorCode": "BAD_REQUEST"})
	}
}
