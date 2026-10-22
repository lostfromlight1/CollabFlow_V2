package middleware

import (
	"net/http"
	"strings"

	"github.com/gin-gonic/gin"
	"github.com/golang-jwt/jwt/v5"
	"github.com/google/uuid"
)

const (
	ContextUserID = "userId"
	ContextEmail  = "email"
)

// JWTAuth verifies Bearer tokens the same way Java services do (HS256, subject = user UUID).
func JWTAuth(secret string) gin.HandlerFunc {
	key := []byte(secret)
	return func(c *gin.Context) {
		header := c.GetHeader("Authorization")
		if !strings.HasPrefix(header, "Bearer ") {
			c.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{
				"success":   false,
				"message":   "Unauthorized",
				"errorCode": "UNAUTHORIZED",
			})
			return
		}
		tokenStr := strings.TrimPrefix(header, "Bearer ")
		token, err := jwt.Parse(tokenStr, func(t *jwt.Token) (any, error) {
			if t.Method != jwt.SigningMethodHS256 {
				return nil, jwt.ErrTokenSignatureInvalid
			}
			return key, nil
		})
		if err != nil || !token.Valid {
			c.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{
				"success":   false,
				"message":   "Unauthorized",
				"errorCode": "UNAUTHORIZED",
			})
			return
		}
		claims, ok := token.Claims.(jwt.MapClaims)
		if !ok {
			c.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{
				"success":   false,
				"message":   "Unauthorized",
				"errorCode": "UNAUTHORIZED",
			})
			return
		}
		sub, _ := claims.GetSubject()
		userID, err := uuid.Parse(sub)
		if err != nil {
			c.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{
				"success":   false,
				"message":   "Unauthorized",
				"errorCode": "UNAUTHORIZED",
			})
			return
		}
		c.Set(ContextUserID, userID)
		if email, _ := claims["email"].(string); email != "" {
			c.Set(ContextEmail, email)
		}
		c.Next()
	}
}

// UserID reads the UUID set by JWTAuth (≈ @CurrentUserId).
func UserID(c *gin.Context) (uuid.UUID, bool) {
	v, ok := c.Get(ContextUserID)
	if !ok {
		return uuid.Nil, false
	}
	id, ok := v.(uuid.UUID)
	return id, ok
}

// Email reads optional email claim from the access token.
func Email(c *gin.Context) string {
	v, ok := c.Get(ContextEmail)
	if !ok {
		return ""
	}
	s, _ := v.(string)
	return s
}
