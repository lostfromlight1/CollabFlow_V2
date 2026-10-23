package config

import (
	"fmt"
	"os"
)

// Config is the document-service equivalent of a Spring @ConfigurationProperties class.
// Files live on local disk (DOCUMENT_STORAGE_DIR). MinIO is not part of the slim
// Compose stack, so the service stores bytes itself and keeps metadata in Postgres.
type Config struct {
	HTTPPort   string
	DBHost     string
	DBPort     string
	DBUser     string
	DBPass     string
	DBName     string
	JWTSecret  string
	StorageDir string
}

func Load() Config {
	return Config{
		HTTPPort:   getenv("DOCUMENT_HTTP_PORT", "9083"),
		DBHost:     getenv("POSTGRES_HOST", "localhost"),
		DBPort:     getenv("POSTGRES_PORT", "5432"),
		DBUser:     getenv("POSTGRES_USER", "postgres"),
		DBPass:     getenv("POSTGRES_PASSWORD", "root"),
		DBName:     getenv("POSTGRES_DB", "collabflow"),
		JWTSecret:  getenv("JWT_SECRET", "ChuPaDupaSecretKeydSQmmDuK70dcClyAHgcdfwedfweftNYqlgPXCTeIpmJ460="),
		StorageDir: getenv("DOCUMENT_STORAGE_DIR", "data/documents"),
	}
}

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
