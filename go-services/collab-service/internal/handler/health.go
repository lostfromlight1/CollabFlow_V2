package handler

import (
	"net/http"

	"github.com/gin-gonic/gin"
)

// Health — if the process is up, DB already connected in main (fail-fast on boot).
func Health(c *gin.Context) {
	c.JSON(http.StatusOK, gin.H{
		"success": true,
		"message": "OK",
		"data": gin.H{
			"service":  "collab-service",
			"status":   "UP",
			"database": "connected",
		},
	})
}
