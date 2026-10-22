package main

import (
	"context"
	"log"
	"os"
	"os/signal"
	"syscall"

	"github.com/collabflow/collab-service/internal/config"
	"github.com/collabflow/collab-service/internal/database"
	"github.com/collabflow/collab-service/internal/handler"
	"github.com/collabflow/collab-service/internal/kafka"
	"github.com/collabflow/collab-service/internal/middleware"
	"github.com/collabflow/collab-service/internal/repository"
	"github.com/collabflow/collab-service/internal/service"
	"github.com/gin-gonic/gin"
)

func main() {
	cfg := config.Load()

	db, err := database.Connect(cfg.DSN())
	if err != nil {
		log.Fatalf("database: %v", err)
	}

	notifRepo := repository.NewNotificationRepository(db)
	inviteRepo := repository.NewInvitationRepository(db)
	commentRepo := repository.NewCommentRepository(db)

	producer := kafka.NewProducer(cfg.KafkaBrokers)
	defer producer.Close()

	notifService := service.NewNotificationService(notifRepo)
	inviteService := service.NewInvitationService(inviteRepo)
	commentService := service.NewCommentService(commentRepo, notifRepo, producer)

	notifHandler := handler.NewNotificationHandler(notifService)
	inviteHandler := handler.NewInvitationHandler(inviteService)
	commentHandler := handler.NewCommentHandler(commentService)

	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	consumer := kafka.NewTaskAssignedConsumer(cfg.KafkaBrokers, cfg.KafkaGroupID, notifService)
	defer consumer.Close()
	go consumer.Run(ctx)

	r := gin.Default()
	v1 := r.Group("/api/v1")
	{
		v1.GET("/collab/health", handler.Health)
		// Public invite peek by token (still needs API key at gateway)
		v1.GET("/invitations/token/:token", inviteHandler.GetByToken)

		auth := v1.Group("")
		auth.Use(middleware.JWTAuth(cfg.JWTSecret))
		{
			auth.GET("/notifications", notifHandler.List)
			auth.PUT("/notifications/read-all", notifHandler.MarkAllRead)
			auth.PUT("/notifications/:id/read", notifHandler.MarkRead)

			auth.POST("/invitations", inviteHandler.Create)
			auth.GET("/invitations", inviteHandler.ListMine)
			auth.GET("/invitations/org", inviteHandler.ListByOrg)
			auth.POST("/invitations/token/:token/accept", inviteHandler.Accept)
			auth.DELETE("/invitations/:id", inviteHandler.Decline)

			auth.POST("/comments", commentHandler.Create)
			auth.GET("/comments", commentHandler.List)
			auth.GET("/comments/:id", commentHandler.Get)
			auth.PUT("/comments/:id", commentHandler.Update)
			auth.DELETE("/comments/:id", commentHandler.Delete)
		}
	}

	addr := ":" + cfg.HTTPPort
	log.Printf("collab-service listening on %s", addr)
	if err := r.Run(addr); err != nil {
		log.Fatal(err)
	}
}
