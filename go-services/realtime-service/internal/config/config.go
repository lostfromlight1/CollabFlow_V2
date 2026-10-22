package config

import (
	"fmt"
	"os"
)

// Config from environment (no Postgres — Kafka + Redis only).
type Config struct {
	HTTPPort     string
	RedisAddr    string
	RedisPass    string
	KafkaBrokers string
	KafkaGroupID string
	JWTSecret    string
}

func Load() Config {
	host := getenv("REDIS_HOST", "localhost")
	port := getenv("REDIS_PORT", "6379")
	return Config{
		HTTPPort:     getenv("REALTIME_HTTP_PORT", "9082"),
		RedisAddr:    fmt.Sprintf("%s:%s", host, port),
		RedisPass:    getenv("REDIS_PASSWORD", ""),
		KafkaBrokers: getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092"),
		KafkaGroupID: getenv("KAFKA_GROUP_ID", "realtime-service"),
		JWTSecret:    getenv("JWT_SECRET", "MdSQmmDuK70dcClyAHgcdfwedfweftNYqlgPXCTeIpmJ460="),
	}
}

func getenv(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}
