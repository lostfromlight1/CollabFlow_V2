package handler

import (
	"strconv"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
)

// organizationIDFromRequest returns X-Organization-Id when set and valid.
// Query organizationId is a fallback for list-by-org endpoints that still accept ?organizationId=.
func organizationIDFromRequest(c *gin.Context) (uuid.UUID, bool) {
	if h := c.GetHeader("X-Organization-Id"); h != "" {
		id, err := uuid.Parse(h)
		if err == nil {
			return id, true
		}
	}
	if q := c.Query("organizationId"); q != "" {
		id, err := uuid.Parse(q)
		if err == nil {
			return id, true
		}
	}
	return uuid.Nil, false
}

// optionalOrgHeader returns only the header (no query fallback) — for filtering inbox lists.
func optionalOrgHeader(c *gin.Context) *uuid.UUID {
	h := c.GetHeader("X-Organization-Id")
	if h == "" {
		return nil
	}
	id, err := uuid.Parse(h)
	if err != nil {
		return nil
	}
	return &id
}

type pageParams struct {
	Page int
	Size int
}

func parsePage(c *gin.Context) pageParams {
	page, _ := strconv.Atoi(c.DefaultQuery("page", "0"))
	size, _ := strconv.Atoi(c.DefaultQuery("size", "20"))
	if page < 0 {
		page = 0
	}
	if size <= 0 {
		size = 20
	}
	if size > 100 {
		size = 100
	}
	return pageParams{Page: page, Size: size}
}

func pageEnvelope(content any, page, size int, total int64) gin.H {
	totalPages := 0
	if size > 0 {
		totalPages = int((total + int64(size) - 1) / int64(size))
	}
	return gin.H{
		"content":       content,
		"page":          page,
		"size":          size,
		"totalElements": total,
		"totalPages":    totalPages,
	}
}
