package kafka

import (
	"context"
	"encoding/json"
	"log"
	"time"

	"github.com/google/uuid"
	kafkago "github.com/segmentio/kafka-go"
)

// Producer publishes domain events (Java analogy: KafkaTemplate after DB commit).
type Producer struct {
	writer *kafkago.Writer
}

func NewProducer(brokersCSV string) *Producer {
	return &Producer{
		writer: &kafkago.Writer{
			Addr:         kafkago.TCP(splitBrokers(brokersCSV)...),
			Balancer:     &kafkago.LeastBytes{},
			RequiredAcks: kafkago.RequireOne,
		},
	}
}

func (p *Producer) Close() error {
	return p.writer.Close()
}

func (p *Producer) Publish(ctx context.Context, topic string, key string, data any) error {
	env := map[string]any{
		"eventId":   uuid.NewString(),
		"eventType": topic,
		"timestamp": time.Now().UTC(),
		"version":   "1.0",
		"data":      data,
	}
	body, err := json.Marshal(env)
	if err != nil {
		return err
	}
	err = p.writer.WriteMessages(ctx, kafkago.Message{
		Topic: topic,
		Key:   []byte(key),
		Value: body,
	})
	if err != nil {
		log.Printf("kafka publish failed topic=%s: %v", topic, err)
		return err
	}
	log.Printf("Published %s key=%s", topic, key)
	return nil
}
