package main

import (
	"context"
	"log"
	"os"
	"os/signal"
	"syscall"

	"github.com/collabflow/realtime-service/internal/config"
	"github.com/collabflow/realtime-service/internal/handler"
	"github.com/collabflow/realtime-service/internal/hub"
	"github.com/collabflow/realtime-service/internal/kafka"
	"github.com/collabflow/realtime-service/internal/redisx"
	"github.com/gin-gonic/gin"
)

func main() {
	cfg := config.Load()

	h := hub.New()
	go h.Run()

	bus := redisx.NewBus(cfg.RedisAddr, cfg.RedisPass, h)
	defer bus.Close()

	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	if err := bus.Ping(ctx); err != nil {
		log.Fatalf("redis: %v (is Docker Redis up on %s?)", err, cfg.RedisAddr)
	}
	go bus.SubscribeAll(ctx)

	taskConsumer := kafka.NewTaskUpdatedConsumer(cfg.KafkaBrokers, cfg.KafkaGroupID, bus)
	commentConsumer := kafka.NewCommentAddedConsumer(cfg.KafkaBrokers, cfg.KafkaGroupID, bus)
	defer taskConsumer.Close()
	defer commentConsumer.Close()
	go taskConsumer.Run(ctx)
	go commentConsumer.Run(ctx)

	ws := handler.NewWSHandler(h, bus, cfg.JWTSecret)

	r := gin.Default()
	v1 := r.Group("/api/v1")
	{
		v1.GET("/realtime/health", handler.Health)
		v1.GET("/realtime/ws", ws.WS)
		v1.POST("/realtime/broadcast", ws.Broadcast)
	}

	addr := ":" + cfg.HTTPPort
	log.Printf("realtime-service listening on %s", addr)
	if err := r.Run(addr); err != nil {
		log.Fatal(err)
	}
}
