package handler

import (
	"net/http"

	"github.com/gin-gonic/gin"
)

// Health proves the process is up (no DB/MinIO required for Wave 7 scaffold).
func Health(c *gin.Context) {
	c.JSON(http.StatusOK, gin.H{
		"success": true,
		"message": "OK",
		"data": gin.H{
			"service": "document-service",
			"status":  "UP",
			"wave":    "7-scaffold",
		},
	})
}
