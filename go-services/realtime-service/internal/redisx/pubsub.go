package redisx

import (
	"context"
	"encoding/json"
	"log"
	"strings"

	"github.com/collabflow/realtime-service/internal/hub"
	"github.com/redis/go-redis/v9"
)

const channelPrefix = "cf:realtime:"

// Bus publishes Kafka-derived events to Redis and fans them into the local Hub.
// Why Redis: if you run 2 realtime pods, Kafka hits one pod; Redis pub/sub reaches all.
type Bus struct {
	rdb *redis.Client
	hub *hub.Hub
}

type envelope struct {
	Room string          `json:"room"`
	Type string          `json:"type"`
	Data json.RawMessage `json:"data"`
}

func NewBus(addr, password string, h *hub.Hub) *Bus {
	rdb := redis.NewClient(&redis.Options{
		Addr:     addr,
		Password: password,
	})
	return &Bus{rdb: rdb, hub: h}
}

func (b *Bus) Ping(ctx context.Context) error {
	return b.rdb.Ping(ctx).Err()
}

func (b *Bus) Close() error {
	return b.rdb.Close()
}

// Publish puts a message on Redis (all realtime instances receive it).
func (b *Bus) Publish(ctx context.Context, room, eventType string, data any) error {
	raw, err := json.Marshal(data)
	if err != nil {
		return err
	}
	body, err := json.Marshal(envelope{Room: room, Type: eventType, Data: raw})
	if err != nil {
		return err
	}
	return b.rdb.Publish(ctx, channelPrefix+room, body).Err()
}

// SubscribeAll listens with a pattern so new rooms work without re-subscribe.
func (b *Bus) SubscribeAll(ctx context.Context) {
	pubsub := b.rdb.PSubscribe(ctx, channelPrefix+"*")
	ch := pubsub.Channel()
	log.Printf("redis PSUBSCRIBE %s*", channelPrefix)
	for {
		select {
		case <-ctx.Done():
			_ = pubsub.Close()
			return
		case msg, ok := <-ch:
			if !ok {
				return
			}
			var env envelope
			if err := json.Unmarshal([]byte(msg.Payload), &env); err != nil {
				log.Printf("redis payload unmarshal: %v", err)
				continue
			}
			room := env.Room
			if room == "" {
				// Fallback: channel name after prefix
				room = strings.TrimPrefix(msg.Channel, channelPrefix)
			}
			out, _ := json.Marshal(hub.OutMessage{Type: env.Type, Room: room, Data: env.Data})
			b.hub.BroadcastLocal(room, out)
		}
	}
}
