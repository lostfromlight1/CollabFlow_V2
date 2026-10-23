package handler

import (
	"net/http"

	"github.com/gin-gonic/gin"
)

// Health proves the HTTP process is up (Kafka consumer runs in the same binary).
func Health(c *gin.Context) {
	c.JSON(http.StatusOK, gin.H{
		"success": true,
		"message": "OK",
		"data": gin.H{
			"service": "email-service",
			"status":  "UP",
			"wave":    "8-scaffold",
			"topic":   "email.send",
		},
	})
}
