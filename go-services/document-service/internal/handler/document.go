package handler

import (
	"net/http"

	"github.com/collabflow/document-service/internal/config"
	"github.com/gin-gonic/gin"
)

// DocumentHandler stubs upload/list until MinIO + document tables land.
// Schema target: docs/database.md (documents, document_folders, …).
//
// TODO(phase-2): MinIO + JWT + org ACL — not UAT demo surface (see docs/fe-contract.md).
type DocumentHandler struct {
	cfg config.Config
}

func NewDocumentHandler(cfg config.Config) *DocumentHandler {
	return &DocumentHandler{cfg: cfg}
}

// UploadStub accepts multipart metadata and returns a placeholder response.
// Does not write to MinIO or Postgres yet — proves the route + config wiring.
// DOC-001 · POST /api/v1/documents/upload
// TODO(phase-2): MinIO + JWT + org ACL — do not demo on UAT.
func (h *DocumentHandler) UploadStub(c *gin.Context) {
	projectID := c.PostForm("projectId")
	entityType := c.PostForm("entityType")
	entityID := c.PostForm("entityId")
	name := c.PostForm("name")
	if name == "" {
		if fh, err := c.FormFile("file"); err == nil && fh != nil {
			name = fh.Filename
		}
	}

	c.JSON(http.StatusAccepted, gin.H{
		"success": true,
		"message": "Upload stub — MinIO not wired yet",
		"data": gin.H{
			"id":         nil,
			"name":       name,
			"projectId":  projectID,
			"entityType": entityType,
			"entityId":   entityID,
			"status":     "STUB",
			"storage": gin.H{
				"endpoint": h.cfg.MinIOEndpoint,
				"bucket":   h.cfg.MinIOBucket,
				"useSSL":   h.cfg.MinIOUseSSL,
			},
		},
	})
}

// ListByProject returns an empty list shaped like future document rows.
// DOC-002 · GET /api/v1/documents?projectId=
// TODO(phase-2): MinIO + JWT + org ACL — do not demo on UAT.
func (h *DocumentHandler) ListByProject(c *gin.Context) {
	projectID := c.Query("projectId")
	if projectID == "" {
		c.JSON(http.StatusBadRequest, gin.H{
			"success": false,
			"message": "projectId query param is required",
		})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"success": true,
		"message": "OK",
		"data": gin.H{
			"projectId": projectID,
			"items":     []any{},
			"total":     0,
			"note":      "Wave 7 scaffold — no DB yet; see docs/database.md documents table",
		},
	})
}
