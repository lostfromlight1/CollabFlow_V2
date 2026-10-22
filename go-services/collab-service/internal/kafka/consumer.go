package kafka

import (
	"context"
	"encoding/json"
	"log"
	"strings"
	"time"

	kafkago "github.com/segmentio/kafka-go"
)

// AssignedHandler is implemented by NotificationService (keeps kafka package free of GORM).
type AssignedHandler interface {
	HandleTaskAssigned(ctx context.Context, env Envelope, data TaskAssignedData) error
}

// Consumer reads task.assigned forever (≈ @KafkaListener loop).
type Consumer struct {
	reader  *kafkago.Reader
	handler AssignedHandler
}

func NewTaskAssignedConsumer(brokersCSV, groupID string, handler AssignedHandler) *Consumer {
	brokers := splitBrokers(brokersCSV)
	reader := kafkago.NewReader(kafkago.ReaderConfig{
		Brokers:        brokers,
		GroupID:        groupID,
		Topic:          "task.assigned",
		MinBytes:       1,
		MaxBytes:       10e6,
		CommitInterval: time.Second,
		StartOffset:    kafkago.LastOffset, // only new messages after this process starts
	})
	return &Consumer{reader: reader, handler: handler}
}

// Run blocks until ctx is cancelled. Call it in a goroutine from main.
func (c *Consumer) Run(ctx context.Context) {
	log.Printf("kafka consumer started topic=task.assigned group=%s", c.reader.Config().GroupID)
	for {
		msg, err := c.reader.FetchMessage(ctx)
		if err != nil {
			if ctx.Err() != nil {
				log.Println("kafka consumer stopped")
				return
			}
			log.Printf("kafka fetch error: %v", err)
			time.Sleep(time.Second)
			continue
		}

		if err := c.handleMessage(ctx, msg); err != nil {
			log.Printf("kafka handle error offset=%d: %v", msg.Offset, err)
			// Do not commit — message can be retried
			continue
		}

		if err := c.reader.CommitMessages(ctx, msg); err != nil {
			log.Printf("kafka commit error: %v", err)
		}
	}
}

func (c *Consumer) Close() error {
	return c.reader.Close()
}

func (c *Consumer) handleMessage(ctx context.Context, msg kafkago.Message) error {
	var env Envelope
	if err := json.Unmarshal(msg.Value, &env); err != nil {
		return err
	}
	var data TaskAssignedData
	if err := json.Unmarshal(env.Data, &data); err != nil {
		return err
	}
	return c.handler.HandleTaskAssigned(ctx, env, data)
}

func splitBrokers(csv string) []string {
	parts := strings.Split(csv, ",")
	out := make([]string, 0, len(parts))
	for _, p := range parts {
		p = strings.TrimSpace(p)
		if p != "" {
			out = append(out, p)
		}
	}
	return out
}
