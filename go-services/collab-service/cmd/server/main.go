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
	"github.com/collabflow/collab-service/internal/platform"
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
	activityRepo := repository.NewActivityRepository(db)

	producer := kafka.NewProducer(cfg.KafkaBrokers)
	defer producer.Close()

	platformClient := platform.NewClient(cfg.PlatformServiceURI, cfg.APIKey)

	notifService := service.NewNotificationService(notifRepo)
	inviteService := service.NewInvitationService(inviteRepo, platformClient, producer, cfg.AppPublicURL)
	commentService := service.NewCommentService(commentRepo, notifRepo, producer)
	activityService := service.NewActivityService(activityRepo)

	notifHandler := handler.NewNotificationHandler(notifService)
	inviteHandler := handler.NewInvitationHandler(inviteService)
	commentHandler := handler.NewCommentHandler(commentService)
	activityHandler := handler.NewActivityHandler(activityService)

	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	// Fan-out: one task.assigned message → notification + activity
	assignedFanout := kafka.FanOutAssigned{
		Handlers: []kafka.AssignedHandler{notifService, activityService},
	}
	assignedConsumer := kafka.NewTaskAssignedConsumer(cfg.KafkaBrokers, cfg.KafkaGroupID, assignedFanout)
	createdConsumer := kafka.NewTaskCreatedConsumer(cfg.KafkaBrokers, cfg.KafkaGroupID, activityService)
	commentConsumer := kafka.NewCommentAddedConsumer(cfg.KafkaBrokers, cfg.KafkaGroupID, activityService)
	defer assignedConsumer.Close()
	defer createdConsumer.Close()
	defer commentConsumer.Close()
	go assignedConsumer.Run(ctx)
	go createdConsumer.Run(ctx)
	go commentConsumer.Run(ctx)
	if cfg.EmailServiceEnabled {
		log.Printf("EMAIL_SERVICE_ENABLED=true — skipping collab email.send stub (use email-service SMTP)")
	} else {
		emailService := service.NewEmailService(notifRepo)
		emailConsumer := kafka.NewEmailSendConsumer(cfg.KafkaBrokers, cfg.KafkaGroupID, emailService)
		defer emailConsumer.Close()
		go emailConsumer.Run(ctx)
	}

	r := gin.Default()
	v1 := r.Group("/api/v1")
	{
		v1.GET("/collab/health", handler.Health)
		v1.GET("/invitations/token/:token", inviteHandler.GetByToken)

		auth := v1.Group("")
		auth.Use(middleware.JWTAuth(cfg.JWTSecret))
		{
			// Static paths before :id siblings
			auth.GET("/notifications/unread-count", notifHandler.UnreadCount)
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

			auth.GET("/activity/entity/:type/:id", activityHandler.ListByEntity)
			auth.GET("/activity/user/:id", activityHandler.ListByUser)
			auth.GET("/activity", activityHandler.List)
		}
	}

	addr := ":" + cfg.HTTPPort
	log.Printf("collab-service listening on %s (platform=%s)", addr, cfg.PlatformServiceURI)
	if err := r.Run(addr); err != nil {
		log.Fatal(err)
	}
}
