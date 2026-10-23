package main

import (
	"log"

	"github.com/collabflow/document-service/internal/auth"
	"github.com/collabflow/document-service/internal/config"
	"github.com/collabflow/document-service/internal/database"
	"github.com/collabflow/document-service/internal/handler"
	"github.com/gin-gonic/gin"
)

func main() {
	cfg := config.Load()
	db, err := database.Connect(cfg.DSN())
	if err != nil {
		log.Fatal(err)
	}
	docs := handler.NewDocumentHandler(db, cfg.StorageDir)

	r := gin.Default()
	v1 := r.Group("/api/v1")
	v1.GET("/documents/health", handler.Health)

	authed := v1.Group("")
	authed.Use(auth.JWTAuth(cfg.JWTSecret))
	authed.POST("/documents/upload", docs.Upload)
	authed.GET("/documents", docs.ListByProject)
	authed.GET("/documents/:id/content", docs.Content)

	addr := ":" + cfg.HTTPPort
	log.Printf("document-service listening on %s storage=%s", addr, cfg.StorageDir)
	if err := r.Run(addr); err != nil {
		log.Fatal(err)
	}
}
