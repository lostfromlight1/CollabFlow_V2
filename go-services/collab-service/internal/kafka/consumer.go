package kafka

import (
	"context"
	"encoding/json"
	"log"
	"strings"
	"time"

	kafkago "github.com/segmentio/kafka-go"
)

// AssignedHandler is implemented by NotificationService and ActivityService.
type AssignedHandler interface {
	HandleTaskAssigned(ctx context.Context, env Envelope, data TaskAssignedData) error
}

// TaskCreatedHandler writes activity (and future side effects) for task.created.
type TaskCreatedHandler interface {
	HandleTaskCreated(ctx context.Context, env Envelope, data TaskCreatedData) error
}

// CommentAddedHandler writes activity for comment.added.
type CommentAddedHandler interface {
	HandleCommentAdded(ctx context.Context, env Envelope, data CommentAddedData) error
}

// FanOutAssigned calls every handler for the same task.assigned message.
// Java analogy: several @KafkaListener methods / ApplicationEvent listeners.
type FanOutAssigned struct {
	Handlers []AssignedHandler
}

func (f FanOutAssigned) HandleTaskAssigned(ctx context.Context, env Envelope, data TaskAssignedData) error {
	for _, h := range f.Handlers {
		if err := h.HandleTaskAssigned(ctx, env, data); err != nil {
			return err
		}
	}
	return nil
}

// Consumer is a single-topic reader loop (≈ @KafkaListener).
type Consumer struct {
	reader *kafkago.Reader
	handle func(ctx context.Context, msg kafkago.Message) error
}

func newConsumer(brokersCSV, groupID, topic string, handle func(ctx context.Context, msg kafkago.Message) error) *Consumer {
	reader := kafkago.NewReader(kafkago.ReaderConfig{
		Brokers:        splitBrokers(brokersCSV),
		GroupID:        groupID,
		Topic:          topic,
		MinBytes:       1,
		MaxBytes:       10e6,
		CommitInterval: time.Second,
		StartOffset:    kafkago.LastOffset, // only new messages after this process starts
	})
	return &Consumer{reader: reader, handle: handle}
}

func NewTaskAssignedConsumer(brokersCSV, groupID string, handler AssignedHandler) *Consumer {
	return newConsumer(brokersCSV, groupID, "task.assigned", func(ctx context.Context, msg kafkago.Message) error {
		env, err := unmarshalEnvelope(msg.Value)
		if err != nil {
			return err
		}
		var data TaskAssignedData
		if err := json.Unmarshal(env.Data, &data); err != nil {
			return err
		}
		return handler.HandleTaskAssigned(ctx, env, data)
	})
}

func NewTaskCreatedConsumer(brokersCSV, groupID string, handler TaskCreatedHandler) *Consumer {
	return newConsumer(brokersCSV, groupID, "task.created", func(ctx context.Context, msg kafkago.Message) error {
		env, err := unmarshalEnvelope(msg.Value)
		if err != nil {
			return err
		}
		var data TaskCreatedData
		if err := json.Unmarshal(env.Data, &data); err != nil {
			return err
		}
		return handler.HandleTaskCreated(ctx, env, data)
	})
}

func NewCommentAddedConsumer(brokersCSV, groupID string, handler CommentAddedHandler) *Consumer {
	return newConsumer(brokersCSV, groupID, "comment.added", func(ctx context.Context, msg kafkago.Message) error {
		env, err := unmarshalEnvelope(msg.Value)
		if err != nil {
			return err
		}
		var data CommentAddedData
		if err := json.Unmarshal(env.Data, &data); err != nil {
			return err
		}
		return handler.HandleCommentAdded(ctx, env, data)
	})
}

func unmarshalEnvelope(raw []byte) (Envelope, error) {
	var env Envelope
	err := json.Unmarshal(raw, &env)
	return env, err
}

// Run blocks until ctx is cancelled. Call it in a goroutine from main.
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

		if err := c.handle(ctx, msg); err != nil {
			log.Printf("kafka handle error topic=%s offset=%d: %v", topic, msg.Offset, err)
			continue
		}

		if err := c.reader.CommitMessages(ctx, msg); err != nil {
			log.Printf("kafka commit error topic=%s: %v", topic, err)
		}
	}
}

func (c *Consumer) Close() error {
	return c.reader.Close()
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
