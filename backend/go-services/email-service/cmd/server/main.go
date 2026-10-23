package main

import (
	"context"
	"log"
	"os"
	"os/signal"
	"syscall"

	"github.com/collabflow/email-service/internal/config"
	"github.com/collabflow/email-service/internal/handler"
	"github.com/collabflow/email-service/internal/kafka"
	"github.com/collabflow/email-service/internal/service"
	"github.com/gin-gonic/gin"
)

func main() {
	cfg := config.Load()
	emailSvc := service.NewEmailService(cfg)

	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	consumer := kafka.NewEmailSendConsumer(cfg.KafkaBrokers, cfg.KafkaGroupID, emailSvc)
	defer consumer.Close()
	go consumer.Run(ctx)

	r := gin.Default()
	r.GET("/api/v1/email/health", handler.Health)
	r.GET("/health", handler.Health)

	addr := ":" + cfg.HTTPPort
	log.Printf(
		"email-service listening on %s kafka=%s group=%s smtp=%s:%s",
		addr, cfg.KafkaBrokers, cfg.KafkaGroupID, cfg.SMTPHost, cfg.SMTPPort,
	)
	if err := r.Run(addr); err != nil {
		log.Fatal(err)
	}
}
