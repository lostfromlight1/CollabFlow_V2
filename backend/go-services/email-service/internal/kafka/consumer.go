package kafka

import (
	"context"
	"encoding/json"
	"log"
	"strings"
	"time"

	kafkago "github.com/segmentio/kafka-go"
)

// EmailSendHandler handles email.send (SMTP-ready log → real SMTP later).
// Java analogy: @KafkaListener on topic email.send.
type EmailSendHandler interface {
	HandleEmailSend(ctx context.Context, env Envelope, data EmailSendData) error
}

// Consumer is a single-topic reader loop (≈ @KafkaListener).
type Consumer struct {
	reader *kafkago.Reader
	handle func(ctx context.Context, msg kafkago.Message) error
}

// NewEmailSendConsumer reads topic email.send with the given consumer group.
func NewEmailSendConsumer(brokersCSV, groupID string, handler EmailSendHandler) *Consumer {
	reader := kafkago.NewReader(kafkago.ReaderConfig{
		Brokers:        splitBrokers(brokersCSV),
		GroupID:        groupID,
		Topic:          "email.send",
		MinBytes:       1,
		MaxBytes:       10e6,
		CommitInterval: time.Second,
		StartOffset:    kafkago.LastOffset,
	})
	return &Consumer{
		reader: reader,
		handle: func(ctx context.Context, msg kafkago.Message) error {
			var env Envelope
			if err := json.Unmarshal(msg.Value, &env); err != nil {
				return err
			}
			var data EmailSendData
			if err := json.Unmarshal(env.Body(), &data); err != nil {
				return err
			}
			return handler.HandleEmailSend(ctx, env, data)
		},
	}
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
