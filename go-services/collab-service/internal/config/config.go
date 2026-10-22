package config

import (
	"fmt"
	"os"
)

// Config holds runtime settings from environment variables.
// Java analogy: application.yml + @Value / @ConfigurationProperties.
type Config struct {
	HTTPPort       string
	DBHost         string
	DBPort         string
	DBUser         string
	DBPass         string
	DBName         string
	KafkaBrokers string
	KafkaGroupID string
	JWTSecret    string
}

// Load reads env with the same local defaults as Java services / .env.example.
func Load() Config {
	return Config{
		HTTPPort:     getenv("COLLAB_HTTP_PORT", "9081"),
		DBHost:       getenv("POSTGRES_HOST", "localhost"),
		DBPort:       getenv("POSTGRES_PORT", "5432"),
		DBUser:       getenv("POSTGRES_USER", "postgres"),
		DBPass:       getenv("POSTGRES_PASSWORD", "root"),
		DBName:       getenv("POSTGRES_DB", "collabflow"),
		KafkaBrokers: getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092"),
		KafkaGroupID: getenv("KAFKA_GROUP_ID", "collab-service"),
		JWTSecret:    getenv("JWT_SECRET", "MdSQmmDuK70dcClyAHgcdfwedfweftNYqlgPXCTeIpmJ460="),
	}
}

// DSN is the Postgres connection string GORM expects.
func (c Config) DSN() string {
	return fmt.Sprintf(
		"host=%s user=%s password=%s dbname=%s port=%s sslmode=disable TimeZone=UTC",
		c.DBHost, c.DBUser, c.DBPass, c.DBName, c.DBPort,
	)
}

func getenv(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}
