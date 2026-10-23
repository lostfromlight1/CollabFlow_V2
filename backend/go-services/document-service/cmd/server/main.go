package main

import (
	"log"

	"github.com/collabflow/document-service/internal/config"
	"github.com/collabflow/document-service/internal/handler"
	"github.com/gin-gonic/gin"
)

func main() {
	cfg := config.Load()
	docs := handler.NewDocumentHandler(cfg)

	r := gin.Default()
	v1 := r.Group("/api/v1")
	{
		v1.GET("/documents/health", handler.Health)
		// Stubs — JWT/org checks land with real storage (Wave 7 follow-up).
		v1.POST("/documents/upload", docs.UploadStub)
		v1.GET("/documents", docs.ListByProject)
	}

	addr := ":" + cfg.HTTPPort
	log.Printf(
		"document-service listening on %s minio=%s bucket=%s",
		addr, cfg.MinIOEndpoint, cfg.MinIOBucket,
	)
	if err := r.Run(addr); err != nil {
		log.Fatal(err)
	}
}
