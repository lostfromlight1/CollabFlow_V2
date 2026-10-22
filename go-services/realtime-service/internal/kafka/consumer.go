package kafka

import (
	"context"
	"encoding/json"
	"log"
	"strings"
	"time"

	"github.com/collabflow/realtime-service/internal/redisx"
	kafkago "github.com/segmentio/kafka-go"
)

type Envelope struct {
	EventID   string          `json:"eventId"`
	EventType string          `json:"eventType"`
	Data      json.RawMessage `json:"data"`
}

type TaskUpdatedData struct {
	TaskID    string `json:"taskId"`
	ProjectID string `json:"projectId"`
	Name      string `json:"name"`
}

type CommentAddedData struct {
	CommentID  string `json:"commentId"`
	EntityType string `json:"entityType"`
	EntityID   string `json:"entityId"`
	AuthorID   string `json:"authorId"`
}

// Consumer turns domain events into Redis publishes (rooms).
type Consumer struct {
	reader *kafkago.Reader
	bus    *redisx.Bus
	handle func(ctx context.Context, env Envelope) error
}

func NewTaskUpdatedConsumer(brokersCSV, groupID string, bus *redisx.Bus) *Consumer {
	return newConsumer(brokersCSV, groupID, "task.updated", bus, func(ctx context.Context, env Envelope) error {
		var d TaskUpdatedData
		if err := json.Unmarshal(env.Data, &d); err != nil {
			return err
		}
		payload := map[string]any{
			"taskId":    d.TaskID,
			"projectId": d.ProjectID,
			"name":      d.Name,
			"eventId":   env.EventID,
		}
		// Clients can subscribe to either room
		_ = bus.Publish(ctx, "project:"+d.ProjectID, "task.updated", payload)
		return bus.Publish(ctx, "task:"+d.TaskID, "task.updated", payload)
	})
}

func NewCommentAddedConsumer(brokersCSV, groupID string, bus *redisx.Bus) *Consumer {
	return newConsumer(brokersCSV, groupID, "comment.added", bus, func(ctx context.Context, env Envelope) error {
		var d CommentAddedData
		if err := json.Unmarshal(env.Data, &d); err != nil {
			return err
		}
		payload := map[string]any{
			"commentId":  d.CommentID,
			"entityType": d.EntityType,
			"entityId":   d.EntityID,
			"authorId":   d.AuthorID,
			"eventId":    env.EventID,
		}
		room := roomForEntity(d.EntityType, d.EntityID)
		return bus.Publish(ctx, room, "comment.added", payload)
	})
}

func roomForEntity(entityType, entityID string) string {
	switch strings.ToUpper(entityType) {
	case "PROJECT":
		return "project:" + entityID
	default:
		return "task:" + entityID
	}
}

func newConsumer(brokersCSV, groupID, topic string, bus *redisx.Bus, handle func(context.Context, Envelope) error) *Consumer {
	reader := kafkago.NewReader(kafkago.ReaderConfig{
		Brokers:        splitBrokers(brokersCSV),
		GroupID:        groupID,
		Topic:          topic,
		MinBytes:       1,
		MaxBytes:       10e6,
		CommitInterval: time.Second,
		StartOffset:    kafkago.LastOffset,
	})
	return &Consumer{reader: reader, bus: bus, handle: handle}
}

func (c *Consumer) Run(ctx context.Context) {
	topic := c.reader.Config().Topic
	log.Printf("kafka consumer started topic=%s group=%s", topic, c.reader.Config().GroupID)
	for {
		msg, err := c.reader.FetchMessage(ctx)
		if err != nil {
			if ctx.Err() != nil {
				log.Printf("kafka consumer stopped topic=%s", topic)
				return
			}
			log.Printf("kafka fetch error topic=%s: %v", topic, err)
			time.Sleep(time.Second)
			continue
		}
		var env Envelope
		if err := json.Unmarshal(msg.Value, &env); err != nil {
			log.Printf("kafka unmarshal topic=%s: %v", topic, err)
			continue
		}
		if err := c.handle(ctx, env); err != nil {
			log.Printf("kafka handle topic=%s: %v", topic, err)
			continue
		}
		if err := c.reader.CommitMessages(ctx, msg); err != nil {
			log.Printf("kafka commit topic=%s: %v", topic, err)
		}
	}
}

func (c *Consumer) Close() error { return c.reader.Close() }

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
