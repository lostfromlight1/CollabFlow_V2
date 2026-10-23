package config

import (
	"fmt"
	"os"
	"strings"

	"github.com/gin-gonic/gin"
)

// Config from environment.
// Postgres is read-only for subscribe ACL (shared DB MVP — same as collab reading platform tables).
type Config struct {
	HTTPPort     string
	RedisAddr    string
	RedisPass    string
	KafkaBrokers string
	KafkaGroupID string
	JWTSecret    string
	DBHost       string
	DBPort       string
	DBUser       string
	DBPass       string
	DBName       string
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
		JWTSecret:    getenv("JWT_SECRET", "ChuPaDupaSecretKeydSQmmDuK70dcClyAHgcdfwedfweftNYqlgPXCTeIpmJ460="),
		DBHost:       getenv("POSTGRES_HOST", "localhost"),
		DBPort:       getenv("POSTGRES_PORT", "5432"),
		DBUser:       getenv("POSTGRES_USER", "postgres"),
		DBPass:       getenv("POSTGRES_PASSWORD", "root"),
		DBName:       getenv("POSTGRES_DB", "collabflow"),
	}
}

// DSN is the Postgres connection string GORM expects.
func (c Config) DSN() string {
	return fmt.Sprintf(
		"host=%s user=%s password=%s dbname=%s port=%s sslmode=disable TimeZone=UTC",
		c.DBHost, c.DBUser, c.DBPass, c.DBName, c.DBPort,
	)
}

// BroadcastAllowed is true only in local/dev (GIN_MODE=debug or APP_ENV=dev).
// Unset GIN_MODE matches Gin's default (debug) so `go run` still works locally.
func BroadcastAllowed() bool {
	if strings.EqualFold(os.Getenv("APP_ENV"), "dev") {
		return true
	}
	mode := os.Getenv("GIN_MODE")
	if mode == "" {
		mode = gin.Mode()
	}
	return mode == gin.DebugMode
}

func getenv(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}
