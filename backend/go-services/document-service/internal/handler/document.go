package handler

import (
	"net/http"
	"path/filepath"
	"time"

	"github.com/collabflow/document-service/internal/auth"
	"github.com/collabflow/document-service/internal/model"
	"github.com/collabflow/document-service/internal/storage"
	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
	"gorm.io/gorm"
)

type DocumentHandler struct {
	db         *gorm.DB
	storageDir string
}

func NewDocumentHandler(db *gorm.DB, storageDir string) *DocumentHandler {
	return &DocumentHandler{db: db, storageDir: storageDir}
}

type documentDTO struct {
	ID         uuid.UUID `json:"id"`
	Name       string    `json:"name"`
	FileType   string    `json:"fileType"`
	FileSize   int64     `json:"fileSize"`
	Version    int       `json:"version"`
	ProjectID  uuid.UUID `json:"projectId"`
	EntityType string    `json:"entityType"`
	EntityID   uuid.UUID `json:"entityId"`
	UploadedBy uuid.UUID `json:"uploadedBy"`
	CreatedAt  time.Time `json:"createdAt"`
}

func toDTO(doc model.Document, projectID uuid.UUID) documentDTO {
	return documentDTO{
		ID:         doc.ID,
		Name:       doc.Name,
		FileType:   doc.FileType,
		FileSize:   doc.FileSize,
		Version:    doc.Version,
		ProjectID:  projectID,
		EntityType: doc.EntityType,
		EntityID:   doc.EntityID,
		UploadedBy: doc.UploadedBy,
		CreatedAt:  doc.CreatedAt,
	}
}

func (h *DocumentHandler) isProjectMember(projectID, userID uuid.UUID) bool {
	var n int64
	h.db.Raw(
		"SELECT COUNT(*) FROM project_members WHERE project_id = ? AND user_id = ?",
		projectID, userID,
	).Scan(&n)
	return n > 0
}

// Upload stores a file and a documents row. DOC-001.
func (h *DocumentHandler) Upload(c *gin.Context) {
	userID, ok := auth.UserID(c)
	if !ok {
		c.JSON(http.StatusUnauthorized, gin.H{"success": false, "message": "Unauthorized"})
		return
	}
	projectID, err := uuid.Parse(c.PostForm("projectId"))
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"success": false, "message": "projectId is required"})
		return
	}
	if !h.isProjectMember(projectID, userID) {
		c.JSON(http.StatusForbidden, gin.H{"success": false, "message": "Project membership required"})
		return
	}
	file, err := c.FormFile("file")
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"success": false, "message": "file is required"})
		return
	}
	name := c.PostForm("name")
	if name == "" {
		name = file.Filename
	}
	name, err = storage.SafeName(name)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"success": false, "message": err.Error()})
		return
	}
	entityType := c.PostForm("entityType")
	if entityType == "" {
		entityType = "PROJECT"
	}
	if entityType != "PROJECT" && entityType != "TASK" && entityType != "WORKSPACE" && entityType != "COMMENT" {
		c.JSON(http.StatusBadRequest, gin.H{"success": false, "message": "invalid entityType"})
		return
	}
	entityID := projectID
	if raw := c.PostForm("entityId"); raw != "" {
		parsed, err := uuid.Parse(raw)
		if err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"success": false, "message": "invalid entityId"})
			return
		}
		entityID = parsed
	}

	src, err := file.Open()
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"success": false, "message": "could not read file"})
		return
	}
	defer src.Close()

	id := uuid.New()
	size, err := storage.Save(h.storageDir, id.String(), src)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"success": false, "message": "could not store file"})
		return
	}

	now := time.Now().UTC()
	doc := model.Document{
		ID:         id,
		Name:       name,
		FilePath:   id.String(),
		FileType:   file.Header.Get("Content-Type"),
		FileSize:   size,
		EntityType: entityType,
		EntityID:   entityID,
		UploadedBy: userID,
		Version:    1,
		IsLatest:   true,
		CreatedAt:  now,
		UpdatedAt:  now,
	}
	if doc.FileType == "" {
		doc.FileType = "application/octet-stream"
	}

	err = h.db.Transaction(func(tx *gorm.DB) error {
		var prev model.Document
		findErr := tx.Where(
			"entity_type = ? AND entity_id = ? AND name = ? AND is_latest = ?",
			entityType, entityID, name, true,
		).First(&prev).Error
		if findErr == nil {
			doc.Version = prev.Version + 1
			doc.ParentDocumentID = &prev.ID
			if err := tx.Model(&prev).Updates(map[string]any{"is_latest": false, "updated_at": now}).Error; err != nil {
				return err
			}
		}
		return tx.Create(&doc).Error
	})
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"success": false, "message": "could not save document"})
		return
	}
	c.JSON(http.StatusCreated, gin.H{"success": true, "message": "Uploaded", "data": toDTO(doc, projectID)})
}

// ListByProject returns the latest file rows for a project. DOC-002.
func (h *DocumentHandler) ListByProject(c *gin.Context) {
	userID, ok := auth.UserID(c)
	if !ok {
		c.JSON(http.StatusUnauthorized, gin.H{"success": false, "message": "Unauthorized"})
		return
	}
	projectID, err := uuid.Parse(c.Query("projectId"))
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"success": false, "message": "projectId query param is required"})
		return
	}
	if !h.isProjectMember(projectID, userID) {
		c.JSON(http.StatusForbidden, gin.H{"success": false, "message": "Project membership required"})
		return
	}
	var rows []model.Document
	if err := h.db.Where(
		"entity_type = ? AND entity_id = ? AND is_latest = ?",
		"PROJECT", projectID, true,
	).Order("created_at desc").Find(&rows).Error; err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"success": false, "message": "could not list documents"})
		return
	}
	items := make([]documentDTO, 0, len(rows))
	for _, row := range rows {
		items = append(items, toDTO(row, projectID))
	}
	c.JSON(http.StatusOK, gin.H{
		"success": true,
		"message": "OK",
		"data": gin.H{
			"projectId": projectID,
			"items":     items,
			"total":     len(items),
		},
	})
}

// Content streams the stored bytes. DOC-003.
func (h *DocumentHandler) Content(c *gin.Context) {
	userID, ok := auth.UserID(c)
	if !ok {
		c.JSON(http.StatusUnauthorized, gin.H{"success": false, "message": "Unauthorized"})
		return
	}
	id, err := uuid.Parse(c.Param("id"))
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"success": false, "message": "invalid id"})
		return
	}
	var doc model.Document
	if err := h.db.First(&doc, "id = ?", id).Error; err != nil {
		c.JSON(http.StatusNotFound, gin.H{"success": false, "message": "Document not found"})
		return
	}
	projectID := doc.EntityID
	if doc.EntityType != "PROJECT" {
		c.JSON(http.StatusForbidden, gin.H{"success": false, "message": "Unsupported entity"})
		return
	}
	if !h.isProjectMember(projectID, userID) {
		c.JSON(http.StatusForbidden, gin.H{"success": false, "message": "Project membership required"})
		return
	}
	c.FileAttachment(filepath.Join(h.storageDir, filepath.Base(doc.FilePath)), doc.Name)
}
